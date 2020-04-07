package com.github.tanqizhou.share.netty.old;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * BIO，同步阻塞IO，阻塞整个步骤，如果连接少，他的延迟是最低的，因为一个线程只处理一个连接，适用于少连接且延迟低的场景，比如说数据库连接。
 */
@Slf4j
public class PlainBioServer {


    public static void main(String[] args) {
        PlainBioServer plainBioServer = new PlainBioServer();
        log.info("========start=========");
        try {
            plainBioServer.serve(8556);
        } catch (IOException e) {
            e.printStackTrace();
            log.info(e.getMessage());
        }
    }

    public void serve(int port) throws IOException {
        final ServerSocket socket = new ServerSocket(port);
        try {
            // 一个线程反复接受连接事件
            while (true) {
                //阻塞，解除阻塞条件是所有请求数据都传输完成
                final Socket clientSocket = socket.accept();
                System.out.println("Accepted connection from " + clientSocket);
                //另一个线程
                new Thread(() -> {
                    OutputStream out;
                    try {
                        out = clientSocket.getOutputStream();
                        String socketString = getSocketString(clientSocket);
                        log.info(socketString);
                        out.write("HTTP/1.1 200 OK\r\n".getBytes(StandardCharsets.UTF_8));
                        out.flush();
                        socketString = getSocketString(clientSocket);
                        log.info(socketString);
                        clientSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        log.info(e.getMessage());
                        try {
                            clientSocket.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            log.info(ex.getMessage());
                        }
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.info(e.getMessage());
        }
    }

    public static String getSocketString(Socket clientSocket) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
        System.out.println("接收服务器反馈: ");
        StringBuffer buffer = new StringBuffer();
        String line = "";
        while ((line = br.readLine()) != null) {
            buffer.append("\n" + line);
        }
        br.close();
        return buffer.toString();
    }
}

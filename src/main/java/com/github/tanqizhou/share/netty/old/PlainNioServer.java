package com.github.tanqizhou.share.netty.old;

import com.github.tanqizhou.share.netty.old.entity.ResponseHeaders;
import com.github.tanqizhou.share.netty.old.entity.StatusCodeEnum;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * NIO，同步非阻塞IO，阻塞业务处理但不阻塞数据接收，适用于高并发且处理简单的场景，比如聊天软件。
 */
@Slf4j
public class PlainNioServer {


    private static String resp = "HTTP/1.1 200 OK\r\n" +
            "Date: Sat, 31 Dec 2005 23:59:59 GMT\r\n" +
            "Content-Type: text/html;charset=UTF-8\r\n" +
            "Content-Length: 71\r\n" + "\r\n";

    //Content-Length 必须与content一致，否则Http错误
    //response  不可以乱码！！！！否则可能接收不到
    private static String content =
            "<html>" +
            "<head>" +
            "<title>Wrox Homepage</title>" +
            "</head>" + "<body>" +
            "渣渣谷歌" +
            "</body>" +
            "</html>";

    //客户端向服务端握手或发送消息
    private int clientToServer = 0;

    //服务端向客户端握手或发送消息
    private int serverToClient = 0;

    public static void main(String[] args) {
        PlainNioServer plainNioServer = new PlainNioServer();
        log.info("========start=========");
        try {
            plainNioServer.serve(8556);
        } catch (IOException e) {
            e.printStackTrace();
            log.info(e.getMessage());
        }
    }
    public void serve(int port) throws IOException {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        ServerSocket ss = serverChannel.socket();
        InetSocketAddress address = new InetSocketAddress(port);
        ss.bind(address);
        Selector selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        final ByteBuffer msg = ByteBuffer.wrap("Hi!\r\n".getBytes());
        for (; ; ) {
            try {
                int readyNum = selector.select();
                log.info("当前key：" + readyNum);
            } catch (IOException ex) {
                ex.printStackTrace();
                // handle exception
                break;
            }
            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = readyKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                try {
                    if (key.isAcceptable()) {
                        clientToServer++;
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();
                        client.configureBlocking(false);
                        client.register(selector,SelectionKey.OP_READ);
                        if (clientToServer == 1 ) {
                            log.info("server 接收 client 的第1次握手，并向客户端握手");
                            serverToClient++;
                        }
                        if (clientToServer == 2 ) {
                            log.info("server 接收 client 的第2次握手，连接建立完成");
                        }
                        log.info("Accepted connection from " + client);
                    }
                    if (key.isReadable()) {
                        clientToServer++;
                        ByteBuffer readBuffer=ByteBuffer.allocate(1024);
                        SocketChannel client = (SocketChannel) key.channel();
                        log.info("client");
                        client.read(readBuffer);
                        readBuffer.flip();
                        if (readBuffer.remaining() == 0) {
                            log.info("读取不可以用");
                            key.cancel();
                            continue;
                        }
                        byte[] bytes=new byte[readBuffer.remaining()];
                        readBuffer.get(bytes);
                        log.info("收到新消息");
                        log.info("\n"+new String(bytes, StandardCharsets.UTF_8));
                        client.register(selector, SelectionKey.OP_WRITE);
                    }
                    if (key.isWritable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        log.info("client");
                        //返回一个结果
                        log.info("准备发送一条消息");
                        ResponseHeaders headers = new ResponseHeaders(StatusCodeEnum.OK.getCode());
                        headers.setContentLength(content.length());
                        String s = headers.toString();
                        int length = content.length();
                        log.info("length:{}",length);
                        String response = resp + content;
                        byte[] writeBytes = response.getBytes(StandardCharsets.UTF_8);
                        ByteBuffer writeBuffer = ByteBuffer.allocate(writeBytes.length);
                        writeBuffer.put(writeBytes);
                        writeBuffer.flip();
                        client.write(writeBuffer);
                        client.close();
                    }
                } catch (IOException ex) {
                    key.cancel();
                    try {
                        key.channel().close();
                    } catch (IOException cex) {
                        // 在关闭时忽略
                    }
                }
            }
        }
    }
}

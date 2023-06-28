package org.mex.kgListener;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 代理服务器
 */
public class Proxy {

    public static void proxy() {
        int localPort = 8083;  // 本地监听端口
        String remoteHost = "gateway.kugou.com";
        int remotePort = 80;  // 目标主机的端口

        try {
            // 创建本地服务器Socket
            ServerSocket serverSocket = new ServerSocket(localPort);
            System.out.println("监听端口 " + localPort + " ...");

            while (true) {
                // 等待客户端连接
                Socket clientSocket = serverSocket.accept();
                System.out.println("客户端连接成功，地址：" + clientSocket.getInetAddress().getHostAddress());
                System.out.println(clientSocket);

                // 创建与目标主机之间的Socket
                Socket serverSocket2 = new Socket(remoteHost, remotePort);

                InputStream inputStream = serverSocket2.getInputStream();
                //打印出来
//                System.out.println("flag1");
//                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
//                StringBuilder stringBuilder = new StringBuilder();
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    stringBuilder.append(line);
//                    System.out.println("flag2");
//                    System.out.println(line);
//                }

                // 创建客户端到目标主机的数据传输线程
                Thread clientToServerThread = new Thread(new TrafficThread(clientSocket.getInputStream(), serverSocket2.getOutputStream()));
                // 创建目标主机到客户端的数据传输线程
                Thread serverToClientThread = new Thread(new TrafficThread(serverSocket2.getInputStream(), clientSocket.getOutputStream()));

                // 启动数据传输线程
                clientToServerThread.start();
                serverToClientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 将数据从一个Socket传输到另一个Socket
    private static class TrafficThread implements Runnable {

        private InputStream input;
        private OutputStream output;

        public TrafficThread(InputStream input, OutputStream output) {
            this.input = input;
            this.output = output;
        }

        @Override
        public void run() {
//            Handle handle = new Handle();
//            InputStream processedInput = handle.song2free(input);
            InputStream processedInput = input;
            try {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = processedInput.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

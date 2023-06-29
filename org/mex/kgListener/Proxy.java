package org.mex.kgListener;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 代理服务器
 */
public class Proxy {

    public static void proxy() {
        int localPort = 8083;  // 本地监听端口

        try {
            // 创建本地服务器Socket
            ServerSocket serverSocket = new ServerSocket(localPort);
            System.out.println("监听端口 " + localPort + " ...");

            while (true) {
                // 等待客户端连接
                Socket clientSocket = serverSocket.accept();
                System.out.println("客户端连接成功，地址：" + clientSocket.getInetAddress().getHostAddress());
                Thread goTun = new Thread(new Tun(clientSocket));
                goTun.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 多个转发器
    private static class Tun implements Runnable {
        String remoteHost;
        int remotePort;  // 目标主机的端口
        private Socket clientSocket;

        public Tun(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                InputStream receive = clientSocket.getInputStream();// 读取客户端发来的数据,注意只能获取一次
                InputStreamReader clientSocketInputStream = new InputStreamReader(receive);
                BufferedReader bufferedReader = new BufferedReader(clientSocketInputStream);
                bufferedReader.mark(4096); // 标记一下起始位置,以便后续重置指针
                String firstLine = bufferedReader.readLine();// 读取第一行
                bufferedReader.reset(); // 重置指针,将指针调到行首以便后续转发流量
                if (firstLine == null || firstLine.length() == 0) {
                    System.out.println("请求为空");
                    return;
                }
                String host = firstLine.split(" ")[1];// 获取请求的主机
//                System.out.println("\t首行" + firstLine + "\n\thost:" + host);
                URL url = null;
                if(firstLine.startsWith("CONNECT")) {
                    if (host.split(":").length == 2) {
                        remoteHost = host.split(":")[0];
                        remotePort = Integer.parseInt(host.split(":")[1]);
                    } else {
                        remoteHost = host;
                        remotePort = 80;
                    }
                } else {
                    url = new URL(host);
                    remoteHost = url.getHost();
                    remotePort = url.getPort() <= 0 ? 80 : url.getPort();
                }
                System.out.println("首行:" + firstLine + "\t请求主机:" + remoteHost + ":" + remotePort);
                if (remoteHost == null || remoteHost.length() == 0 ) {
                    System.out.println(" ! 请求主机为空");
                    return;
                }

                // 创建与目标主机之间的Socket
                Socket serverSocket2 = new Socket(remoteHost, remotePort);
                System.out.println("连接目标主机成功，地址：" + serverSocket2.getInetAddress());

//                  //测试,用于显示服务器的响应
//                InputStream inputStream = serverSocket2.getInputStream();
//                BufferedReader bufferedReader2 = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
//                String firstLine2;
//                while ((firstLine2 = bufferedReader2.readLine()) != null) {
//                    System.out.println("目标主机返回：" + firstLine2);
//                }

                // 创建客户端到目标主机的数据传输线程
                Thread clientToServerThread = new Thread(new TrafficThread(bufferedReader, serverSocket2.getOutputStream()));
                // 创建目标主机到客户端的数据传输线程
                Thread serverToClientThread = new Thread(new TrafficThread(serverSocket2.getInputStream(), clientSocket.getOutputStream()));

                // 启动数据传输线程
                clientToServerThread.start();
                serverToClientThread.start();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("连接目标主机失败");
            }
        }
    }

    // 将数据从一个Socket传输到另一个Socket
    private static class TrafficThread implements Runnable {

        private byte streamCase; // 表示使用哪种流, 0: InputStream, 1: BufferedReader

        private InputStream input;
        private BufferedReader BRinput;
        private OutputStream output;

        public TrafficThread(InputStream input, OutputStream output) {
            this.input = input;
            this.output = output;
            this.streamCase = 0;
        }

        public TrafficThread(BufferedReader BRinput, OutputStream output) {
            this.BRinput = BRinput;
            this.output = output;
            this.streamCase = 1;

            try {
                this.input = BRinputToInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        //把BRinput转换成InputStream
        private InputStream BRinputToInputStream() throws IOException {
            StringBuilder stringBuilder = new StringBuilder();
            char[] buffer = new char[1024];
            int bufferLength;
            while ((bufferLength = BRinput.read(buffer)) != -1) {
                stringBuilder.append(new String(buffer, 0, bufferLength));
            }
            InputStream inputStream = new ByteArrayInputStream(stringBuilder.toString().getBytes());
            return inputStream;
        }

        @Override
        public void run() {
            try {
                switch (streamCase) {
                    case 0 -> transferByInputStream();
                    case 1 -> transferByBufferedReader();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void transferByInputStream() throws IOException {
            StringBuilder stringBuilder = new StringBuilder();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                stringBuilder.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
                System.out.println("转发Stream的流量:\t" + new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
            }
            System.out.println("转发Stream流量:\t" + stringBuilder.toString());
        }

        private void transferByBufferedReader() {
            try {
                char[] charStream = new char[1024];
                int charsRead;
                while ((charsRead = BRinput.read(charStream)) != -1) {
                    byte[] buffer = new String(charStream, 0, charsRead).getBytes();
                    output.write(buffer, 0, buffer.length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

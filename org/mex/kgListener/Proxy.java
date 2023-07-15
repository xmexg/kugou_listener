package org.mex.kgListener;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 代理服务器,无用
 */
public class Proxy {

    public static void proxy() {
        int localPort = 8083;  // 本地监听端口

        try {
            // 创建本地服务器Socket
            ServerSocket serverSocket = new ServerSocket(localPort);
            System.out.println("监听端口 " + localPort + " ...");
            ExecutorService service = Executors.newFixedThreadPool(20); // 创建线程池
            while (true) {
                // 等待客户端连接
                Socket clientSocket = serverSocket.accept();
                System.out.println("客户端连接成功，地址：" + clientSocket.getInetAddress().getHostAddress());
                Thread goTun = new Thread(new Tun(clientSocket));
                service.execute(goTun);
//                goTun.start();
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
//                byte[] bytes = new byte[2048];
//                int len = receive.read(bytes);
//                String result = new String(bytes, 0, len);
//                System.out.println(result);
 /*               InputStream receive_copy = IOUtils.toBufferedInputStream(receive);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = receive.read(buffer)) > -1 ) {
                    baos.write(buffer, 0, len);
                }
                InputStream receive_after = new ByteArrayInputStream(baos.toByteArray());
/*                byte[] buffer = new byte[1024];
                int len;
                while ((len = receive.read(buffer)) > -1 ) {
                    baos.write(buffer, 0, len);
                }
                baos.flush();
                InputStream receive_copy = new ByteArrayInputStream(baos.toByteArray());
                InputStream receive_copy2 = new ByteArrayInputStream(baos.toByteArray());*/
//                InputStreamReader clientSocketInputStream = new InputStreamReader(receive_copy);
//                BufferedReader bufferedReader = new BufferedReader(clientSocketInputStream);
//                bufferedReader.mark(4096); // 标记一下起始位置,以便后续重置指针
/*                String firstLine = bufferedReader.readLine();// 读取第一行
                System.out.println("首行:" + firstLine);
                bufferedReader.reset(); // 重置指针,将指针调到行首以便后续转发流量
/*                if (firstLine == null || firstLine.length() == 0) {
                    System.out.println("请求为空");
                    return;
                }
                String host = firstLine.split(" ")[1];// 获取请求的主机
                System.out.println("\t首行" + firstLine + "\n\thost:" + host);
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
                }*/

                // 创建与目标主机之间的Socket
                Socket serverSocket2 = new Socket("gateway.kugou.com", 80);
                System.out.println("连接目标主机成功，地址：" + serverSocket2.getInetAddress());

//                  //测试,用于显示服务器的响应
//                InputStream inputStream = serverSocket2.getInputStream();
//                BufferedReader bufferedReader2 = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
//                String firstLine2;
//                while ((firstLine2 = bufferedReader2.readLine()) != null) {
//                    System.out.println("目标主机返回：" + firstLine2);
//                }

                // 创建客户端到目标主机的数据传输线程
//                Thread clientToServerThread = new Thread(new TrafficThread(receive, serverSocket2.getOutputStream()));
                // 创建目标主机到客户端的数据传输线程
//                Thread serverToClientThread = new Thread(new TrafficThread(serverSocket2.getInputStream(), clientSocket.getOutputStream()));

                // 启动数据传输线程
//                clientToServerThread.start();
//                serverToClientThread.start();
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
            char[] buffer = new char[1024*3];
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
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            int bytesRead;
//            byte[] buffer = new byte[1024*3];
//            while ((bytesRead = input.read(buffer)) != -1) {
//                baos.write(buffer, 0, bytesRead);
//            }
//            byte[] modifiedData = baos.toByteArray();
//            output.write(modifiedData, 0, modifiedData.length);
//            output.flush();

            input.transferTo(output);

//            DataInputStream in = new DataInputStream(input);
//            DataOutputStream out = new DataOutputStream(output);
//            int length = 0, lengthin = in.available();
//            byte[] alldata = new byte[lengthin];
//            for(byte i : in.readAllBytes()) {
//                alldata[length] = i;
//                length++;
//            }
////            System.out.println("转发Stream的流量长度:\t"+length+"\t"+lengthin);
//            System.out.println("转发Stream的流量:\t" + new String(alldata));
//            out.write(alldata, 0, lengthin);
//            byte[] buffer = new byte[1024];
//            int bytesRead;
//            while ((bytesRead = in.read(buffer)) != -1) {
//                out.write(buffer, 0, bytesRead);
//            }

//            DataInputStream in = new DataInputStream(input);
//            System.out.println("转发Stream的流量:\t" + in.readAllBytes());
//            byte [] one = in.readAllBytes();
//            System.out.println("转发Stream的流量长度:\t"+one.length);
//            output.write(one, 0, one.length);
//            byte[] one = baos.toByteArray();
//            byte[] two = baos.toByteArray();
//            System.out.println(new String(two));
//            output.write(two, 0, two.length);


//            StringBuilder stringBuilder = new StringBuilder();
//            byte[] buffer = new byte[1024*3];
//            int bytesRead;
//            while ((bytesRead = input.read(buffer)) != -1) {
//                output.write(buffer, 0, bytesRead);
//                stringBuilder.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
////                System.out.println("转发Stream的流量:\t" + new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
//            }
//            System.out.println("转发Stream流量:\t" + stringBuilder.toString());
        }

        private void transferByBufferedReader() {
            try {
                char[] charStream = new char[1024*3];
                int charsRead;
                while ((charsRead = BRinput.read(charStream)) != -1) {
                    byte[] buffer = new String(charStream, 0, charsRead).getBytes();
                    output.write(buffer, 0, buffer.length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        private void transferByBufferedReader2() throws IOException {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
        }

    }
}

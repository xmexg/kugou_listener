package org.mex.kgListener;

import com.google.gson.*;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KGProxy {
    public static int localPost = 8083; // 本地监听端口

    KGProxy(int localPost){
        this.localPost = localPost;
    }

    KGProxy(){}

    public void start() {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(localPost);
            System.out.println("监听端口 " + localPost + " ...");
            ExecutorService service = Executors.newFixedThreadPool(20); // 创建线程池

            while (true){
                Socket clientSocket = serverSocket.accept();
                System.out.println("客户端连接成功，地址：" + clientSocket.getInetAddress().getHostAddress());
                Thread goTun = new Thread(new NetTun(clientSocket));
                service.execute(goTun);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static class NetTun implements Runnable {

        private Socket socketIn,socketOut;
        private InputStream isIn,isOut;
        private OutputStream osIn,osOut;

        private String ipv4_regex = "^((\\d{1,3}\\.){3}\\d{1,3})$"; // 酷狗的ipv4地址,都是无效地址,应直接舍弃

        public NetTun (Socket socket) {
            this.socketIn = socket;
        }

        @Override
        public void run() {
            try {
                isIn = socketIn.getInputStream();
                osIn = socketIn.getOutputStream();

                byte[] link_byte = new byte[1024*3];
                int link_byte_length = isIn.read(link_byte);
//                String[] link_info = getLink(link_byte);
//                System.out.println("host:"+link_info[0]+"\tport:"+link_info[1]);
//
//                socketOut = new Socket(link_info[0], Integer.parseInt(link_info[1]));

                Tun_link link = getLink(link_byte);
                if(link.host.length() == 0 || link.port == 0 || link.host.matches(ipv4_regex)){
//                    System.out.println("host or port is null");
                    return;
                }

//                System.out.println("host:"+link.host+"\tport:"+link.port+"\tmethod:"+link.method+"\turl:"+link.url);

                // 替换vip歌曲
                if(link.url.startsWith("http://tracker.kugou.com/v3/url?IsFreePart")){
                    System.out.println("请求:" + new String(link_byte, 0, link_byte_length));
                    String vip_transplant = new VIPMusic_transplant().transplant(new String(link_byte, 0, link_byte_length), link.url);
                    System.out.println("==========\n替换vip歌曲:\n -> "+link.url+"\n <- "+vip_transplant+"\n==========");
                    byte[] vip_transplant_byte = vip_transplant.getBytes();
                    osIn.write(vip_transplant_byte);
                    return;
                }

                socketOut = new Socket(link.host, link.port);
                isOut = socketOut.getInputStream();
                osOut = socketOut.getOutputStream();

                // 转发请求
                new Thread(() -> {
                    try {
                        byte[] bytes = new byte[2048];
                        osOut.write(link_byte);
                        int len;
                        while ((len = isIn.read(bytes)) > -1) {
                            osOut.write(bytes, 0, len);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

                // 转发响应
                new Thread(() -> {
                    try {
                        byte[] bytes = new byte[2048];
                        int len;
                        while ((len = isOut.read(bytes)) > -1) {
//                            System.out.println(new String(bytes));
                            osIn.write(bytes, 0, len);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 在流中解析出要转发到远程的host和port
    private static Tun_link getLink(byte[] b) throws MalformedURLException {
        Tun_link tun_link = new Tun_link();
        String text = new String(b);
        String remoteHost;
        int remotePort;
        int space_ind = text.indexOf(" "); // 第一个空格的位置
        String method = text.substring(0, space_ind); // 截取第一个空格前的字符串
        String host = text.substring(space_ind+1);// 截取第一次出现空格后的字符串
        if(host.indexOf(" ") != -1){ // 如果还有空格,空格前的为链接
            host = host.substring(0, host.indexOf(" "));
        }
        String url_link = host; // 完整链接
        if(text.startsWith("CONNECT")) { // https,舍弃即可
            if (host.split(":").length == 2) {
                remoteHost = host.split(":")[0];
                remotePort = Integer.parseInt(host.split(":")[1]);
            } else {
                remoteHost = host;
                remotePort = 80;
            }
        } else {
            URL url = new URL(host);
            remoteHost = url.getHost();
            remotePort = url.getPort() <= 0 ? 80 : url.getPort();
        }
//        return new String[]{remoteHost, String.valueOf(remotePort)};
        tun_link.host = remoteHost;
        tun_link.port = remotePort;
        tun_link.method = method;
        tun_link.url = url_link;
        return tun_link;
    }
}

class Tun_link{
    public String host;
    public int port;
    public String method;
    public String url;

    Tun_link(String host, int port, String method, String url){
        this.host = host;
        this.port = port;
        this.method = method;
        this.url = url;
    }

    Tun_link(){}
}

// 解析vip试听部分，把歌曲地址转成其他平台的地址
class VIPMusic_transplant{
    public String transplant(String HEAD, String url){
        Simple_http simple_http = new Simple_http();
        HashMap<String, String> headers = new HashMap<>();
        for (String s : HEAD.split("\n")) {
            if(s.contains(":")){
                headers.put(s.split(":")[0], s.split(":")[1]);
            }
        }
        String[] response = simple_http.get(headers, url);
        System.out.print("原始：");
        System.out.println(response[1]);
//        把response转成json格式，然后解析出url
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()  // 禁用HTML转义
                .create();
        JsonObject jsonObject = gson.fromJson(response[1], JsonObject.class);
        String song_name = jsonObject.get("fileName").getAsString();
        String song_backupUrl = jsonObject.get("backupUrl").getAsJsonArray().get(0).getAsString();
        String song_url = jsonObject.get("url").getAsString();
        System.out.println("歌曲名："+song_name+"\n歌曲地址1: "+song_backupUrl+"\n歌曲地址2："+song_url);
//        song_backupUrl = "http://fspc.tx.kugou.com/202307151416/7d11d5b7f2c12c4e5b4f89153adc1c23/v3/2c9cba0a0c9432b1620b1376b03523db/yp/p_0_1662334/a1001_u1038506433_d0unmyc3sbu7a35qf1t0kgjfl_p1_s2542332105.mp3";
//        song_url = "http://fspc.hw.kugou.com/202307151416/451574bca672bf7eb1ec5a920debe96b/v3/2c9cba0a0c9432b1620b1376b03523db/yp/p_0_1662334/a1001_u1038506433_d0unmyc3sbu7a35qf1t0kgjfl_p1_s2542332105.mp3";
//        JsonArray song_backupUrl_array = new JsonArray();
//        song_backupUrl_array.add(song_backupUrl);
//        jsonObject.add("backupUrl", song_backupUrl_array);
//        jsonObject.addProperty("url", song_url);
//        response = jsonObject.toString();
//        response = response.replace("/", "\\/");
//        response = response.replace("\\/", "/");
        String finish_response = response[0]+"\n"+response[1];
        return finish_response;
    }
}

// 简单的http请求
class Simple_http{
    public String[] get(HashMap HEAD, String url){
        try {
            URL url1 = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) url1.openConnection();
            for (Object o : HEAD.keySet()) {
                connection.setRequestProperty(o.toString(), HEAD.get(o).toString());
            }
            connection.setRequestProperty("Accept-Encoding", "identity"); // 始终不压缩
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            // 获取响应头
            Map<String, List<String>> headerFields = connection.getHeaderFields();
            StringBuffer header = new StringBuffer();
            for (String s : headerFields.keySet()) {
                if(s != null){
                    header.append(s).append(":");
                } else {
                    for (String s1 : headerFields.get(s)) {
                        header = new StringBuffer(s1).append("\n").append(header);
                    }
                    continue;
                }
                for (String s1 : headerFields.get(s)) {
                    header.append(" ").append(s1);
                }
                header.append("\n");
            }
//            System.out.println("响应头：\n"+header);
            if(connection.getResponseCode() == 200){
                InputStream inputStream = connection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null){
                    stringBuilder.append(line);
                }
                return new String[]{header.toString(), stringBuilder.toString()};
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
package org.mex.kgListener;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        System.out.println("Kugou Listener !");
        System.out.println("酷狗音乐解锁vip歌曲\n1.启动该程序\n2.酷狗音乐设置代理,127.0.0.1:8083");
        new Proxy().proxy();
//        try {
//            new Test().start();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }
}

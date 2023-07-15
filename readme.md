# kugou_Listener
这是个半成品，目前无法运行，已弃坑
替换试听歌曲的url实现听完整版歌曲

# 目前进度
查看KGProxy.java -> class VIPMusic_transplant -> String transplant(String HEAD, String url)
1. 完善过程,当前酷狗客户端无法识别该方法的流量
2. 等待替换掉试听歌曲的url

# 食用方法
 1. 启动该程序 
 2. 酷狗音乐设置代理为 http，127.0.0.1，8083  

# 原理
用别地资源替换试听歌曲的url实现听完整版歌曲  
[笔记](笔记.txt)
![image](./image/show.png)
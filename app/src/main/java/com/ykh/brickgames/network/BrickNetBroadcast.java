package com.ykh.brickgames.network;

import android.os.Handler;
import android.os.Message;

import com.ykh.brickgames.myViews.GameScreen;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class BrickNetBroadcast {

    public static boolean enable = false;
    public static GameScreen.Adapter data;
    public static int[][] screenData = new int[10][20];     // 客户端使用的屏幕数据
    public static boolean clientEnabled = false;
    public static boolean clientOk = false;
    public static GameScreen screen;
    private static InetAddress address;
    private static byte[] imageBuffer = new byte[200];
    private static MulticastSocket clientMulticastSocket;
    private static MulticastSocket serverMulticastSocket;
    private static Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            screen.invalidate();
            return true;
        }
    });
    private static byte[] bufferLastTime = new byte[8];     // 上一次时间 byte
    private static long lastImageTime = 0L;           // 上一次图像时间
    private static long currentImageTime = 0L;              // 上一次时间
    private static long lastRealTime = 0L;
    private static long currentRealTime = 0L;
    private static long sendTime;
    private static long lastSendTime = 0L;

    static {
        imageBuffer[0] = -5;     // -5代表关闭
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 20; j++) {
                screenData[i][j] = 0;
            }
        }
    }

    /**
     * 生成图像 for 服务器
     */
    public static void generate() {
        sendTime = System.currentTimeMillis();
        if(sendTime - lastSendTime >= 40) {  // 控制发送频率
            int k = 0;
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 20; j++) {
                    imageBuffer[k] = (byte) data.getScreenData(i, j);
                    k++;
                }
            }
            lastSendTime = sendTime;
        }
    }

    /**
     * 服务器发送端
     */
    public static void onServerBroadcastSend() {
        try {
            // 侦听的端口
            serverMulticastSocket = new MulticastSocket(8082);
            // 使用D类地址，该地址为发起组播的那个ip段，即侦听10001的套接字
            address = InetAddress.getByName("239.0.0.1");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    enable = true;      // 把标志打开
                    while (enable) {
                        while (imageBuffer[0] == -5 && enable) { // (-5表示无数据)
                            try {
                                Thread.sleep(5);
                            } catch (InterruptedException ignored) {
                            }
                        }
                        if (enable) {
                            byte[] buf = new byte[208];     // 待发送数据缓冲区
                            System.arraycopy(longToBytes(sendTime), 0, buf, 0, 8);  // 向buf的前8个字节
                            System.arraycopy(imageBuffer, 0, buf, 8, 200);
                            imageBuffer[0] = -5;            // 清除数据(-5表示无数据)
                            // 组报
                            DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                            // 向组播ID，即接收group /239.0.0.1  端口 10001
                            datagramPacket.setAddress(address);
                            // 发送的端口号
                            datagramPacket.setPort(10001);
                            try {
                                // 开始发送
                                serverMulticastSocket.send(datagramPacket);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // long转byte数组
    private static byte[] longToBytes(long num) {
        byte[] b = new byte[8];
        for (int i = 0; i < 8; i++) {
            b[i] = (byte) (num >>> (56 - i * 8));
        }
        return b;
    }

    //byte数组转成long
    private static long byteToLong(byte[] b) {
        long s;
        long s0 = b[7] & 0xff;// 最低位
        long s1 = b[6] & 0xff;
        long s2 = b[5] & 0xff;
        long s3 = b[4] & 0xff;
        long s4 = b[3] & 0xff;// 最低位
        long s5 = b[2] & 0xff;
        long s6 = b[1] & 0xff;
        long s7 = b[0] & 0xff;

        // s0不变
        s1 <<= 8;
        s2 <<= 16;
        s3 <<= 24;
        s4 <<= 8 * 4;
        s5 <<= 8 * 5;
        s6 <<= 8 * 6;
        s7 <<= 8 * 7;
        s = s0 | s1 | s2 | s3 | s4 | s5 | s6 | s7;
        return s;
    }

    /**
     * 客户端接收数据的程序
     * 客户端好像只有收到消息后才能发送
     */
    public static void onClientBroadcastReceiver() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    clientEnabled = true;
                    clientMulticastSocket = new MulticastSocket(10001);         // 接收数据时需要指定监听的端口号
                    InetAddress address = InetAddress.getByName("239.0.0.1");   // 创建组播ID地址
                    clientMulticastSocket.joinGroup(address);                   // 加入地址
                    byte[] buf = new byte[208];                                 // 包长
                    while (clientEnabled) {
                        // 数据报
                        final DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                        clientMulticastSocket.receive(datagramPacket);                  // 接收数据，同样会进入阻塞状态
                        final byte[] message = new byte[200];                           // 从buffer中截取收到的数据
                        System.arraycopy(buf, 8, message, 0, 200);                      // 拷贝图像
                        int k = 0;
                        for (int i = 0; i < 10; i++) {
                            for (int j = 0; j < 20; j++) {
                                if (message[k] < 0) {
                                    screenData[i][j] = 0;
                                } else if (message[k] > 10) {
                                    screenData[i][j] = 10;
                                } else
                                    screenData[i][j] = message[k];
                                k++;
                            }
                        }
                        System.arraycopy(buf, 0, bufferLastTime, 0, 8); // 拷贝时间
                        currentImageTime = byteToLong(bufferLastTime);
                        currentRealTime = System.currentTimeMillis();
                        long diff1 = currentImageTime - lastImageTime;
                        long diff2 = currentRealTime - lastRealTime;
                        if (diff1 > 0) {
                            long d = diff1 - diff2;
                            if (d > 0 && d < 3000) {        // 暂时认为其它数据出错 todo
                                try {
                                    Thread.sleep(d);
                                } catch (InterruptedException ignored) {

                                }
                            }
                            lastImageTime = currentImageTime;
                            lastRealTime = currentRealTime;
                            handler.sendEmptyMessage(0);
                        }
                        clientOk = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    clientOk = false;
                }
            }
        }).start();
    }

    /**
     * 客户端发送数据的代码
     */
    private static void onClientBroadcastSend(InetAddress address) {
        // 假设 239.0.0.1 已经收到了来自其他组ip段的消息，为了进行二次确认，发送 "snoop"
        // 进行确认，当发送方收到该消息可以释放资源
        String out = "我是客户端, 看到我你就成功了!!";
        // 获取"snoop"的字节数组
        byte[] buf = out.getBytes();
        // 组报
        DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
        // 设置地址，该地址来自onBrodacastReceiver()函数阻塞数据报，datagramPacket.getAddress()
        datagramPacket.setAddress(address);
        // 发送的端口号
        datagramPacket.setPort(8082);
        try {
            // 开始发送
            clientMulticastSocket.send(datagramPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 服务器接收端
     */
    private void onServerBroadcastReceiver() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 字节数组的格式，即最大大小
                    byte[] buf = new byte[1024];
                    while (enable) {
                        // 组报格式
                        final DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                        // 接收来自group组播10001端口的二次确认，阻塞
                        serverMulticastSocket.receive(datagramPacket);
                        // 从buf中截取收到的数据
                        final byte[] message = new byte[datagramPacket.getLength()];
                        // 数组拷贝
                        System.arraycopy(buf, 0, message, 0, datagramPacket.getLength());
                        // todo recover runonuithread
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                System.out.println(datagramPacket.getAddress());
//                                // 打印组播端口10001发送过来的消息
//                                System.out.println(new String(message));
//                                // 这里可以根据结接收到的内容进行分发处理，假如收到 10001的 "snoop"字段为关闭命令，即可在此处关闭套接字从而释放资源
//                                edtServerRecv.setText(datagramPacket.getAddress() + " : " + new String(message));
//                            }
//                        });
                        // 这里打印ip字段
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}

package com.ykh.brickgames.network;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.ykh.brickgames.Lg;
import com.ykh.brickgames.services.ClientService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

import static com.ykh.brickgames.network.ServerThread.parseInstruction;

/**
 */
public class ClientThread extends Thread {
    public static final int INPUT = 0xa1;
    public static final int OUTPUT = 0xa2;
    public static final int NETWORK_OUT_OF_TIME = 0xa3;
    public static final int NETWORK_NOT_AVAILABLE = 0xa4;
    public static final int CONNECT_SUCCESSFUL = 0xa5;
    public static final int DISCONNECTED = 0xa6;
    // 定义接收UI线程的消息的Handler对象[消息接收]
    public Handler inHandler;
    // 定义向UI线程发送消息的Handler对象[消息发送]
    private Handler outHandler;
    public static ClientThread mThis;

    // 该线程所处理的Socket所对应的输入流
    private DataInputStream in = null;   // 输入流
    private DataOutputStream out = null;     // 输出流
    private String ip;
    private int port;
    private Socket s;
    private String name;
    private boolean enabled = false;

    /**
     * @param ip         IP地址
     * @param name       玩家名称
     * @param port       端口号
     * @param outHandler 输出的处理器
     */
    public ClientThread(String ip, String name, int port, Handler outHandler) {
        this.outHandler = outHandler;
        this.ip = ip;
        this.port = port;
        this.name = name;
        mThis = this;
    }

    @Override
    public void run() {
        try {
            s = new Socket(ip, port);
            in = new DataInputStream(s.getInputStream());
            out = new DataOutputStream(s.getOutputStream());
            try {
                out.writeUTF(name);
                Lg.e("客户端", "已发送数据");
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 启动一条子线程来读取服务器响应的数据
            new Thread() {
                @Override
                public void run() {
                    String content;
                    enabled = true;
                    // 不断读取Socket输入流中的内容。
                    try {
                        outHandler.sendEmptyMessage(CONNECT_SUCCESSFUL);
                        while (enabled) {
                            content = in.readUTF();
                            // 每当读到来自服务器的数据之后，发送消息通知程序界面显示该数据
                            Message msg = new Message();
                            msg.what = OUTPUT;
                            msg.obj = content;
                            outHandler.sendMessage(msg);
                            Lg.e(msg.obj.toString());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        outHandler.sendEmptyMessage(DISCONNECTED);
                        try {
                            s.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
            // 为当前线程初始化Looper
            Looper.prepare();
            // 创建revHandler对象
            inHandler = new MyHandler();
            // 启动Looper
            Looper.loop();
        } catch (SocketTimeoutException e1) {
            System.out.println("网络连接超时！！");
            outHandler.sendEmptyMessage(NETWORK_OUT_OF_TIME);
            try {
                s.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            outHandler.sendEmptyMessage(NETWORK_NOT_AVAILABLE);
            try {
                s.close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            // 接收到UI线程中用户输入的数据
            if (msg.what == INPUT) {
                // 将用户在文本框内输入的内容写入网络
                try {
                    out.writeUTF(msg.obj.toString());
                } catch (Exception e) {
                    outHandler.sendEmptyMessage(NETWORK_NOT_AVAILABLE);
                    e.printStackTrace();
                }
                try {
                    out.writeUTF("LEFT");
                    Lg.e("客户端", "已发送数据");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (msg.what == ClientService.CLOSE_CLIENT_THREAD) {
                enabled = false;
                try {
                    s.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if(msg.what == ClientService.INSTRUCTION) {
                try {
                    Lg.e("客户端的网络部分正在发送指令...");
                    out.writeUTF(parseInstruction(msg.arg1));
                } catch (IOException e) {
                    outHandler.sendEmptyMessage(NETWORK_NOT_AVAILABLE);
                    e.printStackTrace();
                }
            }
        }
    }
}
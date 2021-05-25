package com.ykh.brickgames.network;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import com.ykh.brickgames.Lg;
import com.ykh.brickgames.R;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;

public class ServerThread extends Thread {
    public final static int NEW_PLAYER_CONNECTED = 0x91;
    public static final int INSTRUCTIONS_FROM_SERVER = 0xf0;
    public static final int ONE_PLAYER_DISCONNECT = 0xf1;
    public static final int GO_UP = 0xf2;
    public static final int GO_LEFT = 0xf3;
    public static final int GO_RIGHT = 0xf4;
    public static final int GO_DOWN = 0xf5;
    public static final int GO_FIRE = 0xf6;
    public static Handler handler;
    //创建一个Vector对象，用于存储客户端发过来的消息
    private final int PORT;
    //负责接收客户端发来的消息,存储所有与服务器端建立连接的客户端
    private final Vector<ClientThread> clients;
    public boolean enabled = false;
    private InetAddress myIPaAddress = null;
    private ServerSocket serverSocket;
    private Context context;

    public ServerThread(Context context, int port) throws Exception {
        clients = new Vector<>();
        PORT = port;
        this.context = context;
        serverSocket = new ServerSocket(PORT);
    }

    /**
     * 获得指令
     */
    private static int parseString(String instruct) {
        switch (instruct) {
            case "left":
                return GO_LEFT;
            case "right":
                return GO_RIGHT;
            case "up":
                return GO_UP;
            case "down":
                return GO_DOWN;
            case "fire":
                return GO_FIRE;
            default:
                return 0;
        }
    }

    public static String parseInstruction(int instruct) {
        switch (instruct) {
            case GO_LEFT:
                return "left";
            case GO_RIGHT:
                return "right";
            case GO_UP:
                return "up";
            case GO_DOWN:
                return "down";
            case GO_FIRE:
                return "fire";
            default:
                return "";
        }
    }

    @Override
    public void run() {
        try {
            myIPaAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e1) {
            return;
        }
        Lg.e("端口号:" + String.valueOf(serverSocket.getLocalPort()));
        enabled = true;
        while (enabled) {
            try {
                // 获取客户端连接，并返回一个新的Socketd对象
                Socket socket = serverSocket.accept();
                if (clients.size() > 3) {
                    // 向子线程发送拒绝的消息
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF("player limited");
                    socket.close();
                } else {
                    // 创建ClientThread线程，用于监听该连接对应的客户端是否发送消息过来，并获取消息
                    ClientThread clientThread = new ClientThread(socket, this, clients.size() + 2);
                    clientThread.start();
                    synchronized (clients) {
                        clients.addElement(clientThread);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 依次检查多个子线程, 将其关闭
        synchronized (clients) {
            for (ClientThread thread : clients) {
                thread.close();
            }
        }
    }

    public void close() {
        enabled = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void finalize() throws Throwable {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        serverSocket = null;
        super.finalize();
    }

    /**
     * 内部类 与客户端通信的线程
     */
    private class ClientThread extends Thread {
        private DataOutputStream out = null;
        private Socket clientSocket;
        private DataInputStream in = null;
        private ServerThread serverThread;
        private boolean enabled = true;
        /**
         * 坦克编号的范围是2 3 4 分别对应右上, 左下, 右下
         */
        private int id;

        /**
         * 构造函数
         * 打开输入输出流, 然后读取IP、玩家昵称等然后发送广播
         */
        ClientThread(Socket socket, ServerThread serverThread, int id) {
            clientSocket = socket;
            this.serverThread = serverThread;
            try {
                in = new DataInputStream(clientSocket.getInputStream());
                out = new DataOutputStream(clientSocket.getOutputStream());
                this.id = id;
                // 向ServerActivity发送广播
                Intent intent = new Intent(context.getResources().getString(R.string.server_receiver_action));
                intent.putExtra("type", NEW_PLAYER_CONNECTED);
                intent.putExtra("ip", clientSocket.getInetAddress().getHostAddress());
                intent.putExtra("id", id);
                intent.putExtra("name", in.readUTF());          // 这里有一次读取 可能会卡住
                context.sendBroadcast(intent);
            } catch (IOException e) {
                e.printStackTrace();
                Lg.e("发送异常，建立I/O通道失败");
            }
        }

        @Override
        public void run() {
            while (enabled) {
                try {
                    Lg.e("服务器在接收数据处阻塞");
                    String text = in.readUTF();
                    Lg.e("服务器收到数据", text);
                    if (handler != null) {
                        Message message = new Message();
                        message.what = INSTRUCTIONS_FROM_SERVER;
                        message.arg1 = id;
                        message.arg2 = parseString(text);
                        handler.sendMessage(message);
                    }
                } catch (IOException e) {
                    enabled = false;        // 关闭服务器
                    Intent intent = new Intent(context.getString(R.string.server_receiver_action));
                    intent.putExtra("type", ONE_PLAYER_DISCONNECT);
                    intent.putExtra("id", id);
                    context.sendBroadcast(intent);
                    Lg.e("服务器检测到客户端断开连接!");
                    e.printStackTrace();
                }
            }
            synchronized (clients) {
                clients.remove(this);
            }
        }

        void close() {
            enabled = false;
        }
    }
}

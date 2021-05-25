package com.ykh.brickgames.services;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

import com.ykh.brickgames.Lg;
import com.ykh.brickgames.R;
import com.ykh.brickgames.network.ClientThread;

import java.util.List;

public class ClientService extends Service {
    public static final int CLOSE_CLIENT_THREAD = 0xe0;
    public static final int INSTRUCTION = 0xe1;
    public static final int DISCONNECT = 0xe2;
    private ClientThread thread;
    private BroadcastReceiver receiver;

    public ClientService() {
    }

    /**
     * 判断服务是否后台运行
     *
     * @param mContext Context
     * @return true表示在运行 false表示不在运行
     */
    public static boolean isServiceRun(Context mContext) {
        boolean isRun = false;
        ActivityManager activityManager = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager
                .getRunningServices(40);
        int size = serviceList.size();
        for (int i = 0; i < size; i++) {
            if (serviceList.get(i).service.getClassName().equals("com.ykh.brickgames.services.ClientService")) {
                isRun = true;
                break;
            }
        }
        return isRun;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getIntExtra("type", -1)) {
                    case INSTRUCTION:
                        thread.inHandler.sendEmptyMessage(ClientThread.INPUT);
                        break;
                    case DISCONNECT:
                        stopSelf();
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter(getString(R.string.client_service_action));
        registerReceiver(receiver, filter);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        thread = new ClientThread(intent.getStringExtra("ip"),
                intent.getStringExtra("player"),
                ServerService.PORT,
                new MyHandler(this));
        thread.start();
    }

    @Override
    public void onDestroy() {
        if (thread.inHandler != null) {
            thread.inHandler.sendEmptyMessage(CLOSE_CLIENT_THREAD);
        }
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        super.onDestroy();
    }


    private static class MyHandler extends Handler {
        ClientService context;

        MyHandler(ClientService context) {
            this.context = context;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == ClientThread.OUTPUT) {
                String message = msg.obj.toString();
                Toast.makeText(context, "客户端收到服务器发来的消息!\n" + message, Toast.LENGTH_SHORT).show();
            } else if (msg.what == ClientThread.NETWORK_NOT_AVAILABLE) {
                Intent intent = new Intent(context.getString(R.string.server_receiver_action));
                intent.putExtra("type", ClientThread.NETWORK_NOT_AVAILABLE);
                context.sendBroadcast(intent);
                context.stopSelf();         // 出现网络问题, 停止服务
            } else if (msg.what == ClientThread.NETWORK_OUT_OF_TIME) {
                Intent intent = new Intent(context.getString(R.string.server_receiver_action));
                intent.putExtra("type", ClientThread.NETWORK_OUT_OF_TIME);
                context.sendBroadcast(intent);
                context.stopSelf();         // 出现网络问题, 停止服务
            } else if (msg.what == ClientThread.CONNECT_SUCCESSFUL) {
                Lg.e("客户端线程发来了连接成功的消息");
                Intent intent = new Intent(context.getString(R.string.server_receiver_action));
                intent.putExtra("type", ClientThread.CONNECT_SUCCESSFUL);
                context.sendBroadcast(intent);  // 连接成功!
            } else if (msg.what == ClientThread.DISCONNECTED) {
                Intent intent = new Intent(context.getString(R.string.server_receiver_action));
                intent.putExtra("type", ClientThread.DISCONNECTED);
                context.sendBroadcast(intent);  // 连接成功!
            }
        }
    }
}

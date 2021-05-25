package com.ykh.brickgames.services;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.ykh.brickgames.R;
import com.ykh.brickgames.network.ServerThread;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerService extends Service {
    // 服务器已经打开
    public static final int SERVER_OPEN_SUCCESS = 1;
    public static final int SERVER_OPEN_FAILED = 2;
    public static int PORT = 10138;
    ServerThread serverThread;

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
            if (serviceList.get(i).service.getClassName().equals("com.ykh.brickgames.services.ServerService")) {
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
    public void onDestroy() {
        serverThread.close();
        super.onDestroy();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        try {
            serverThread = new ServerThread(this, PORT);
            serverThread.start();
            Intent i = new Intent(getResources().getString(R.string.server_receiver_action));
            i.putExtra("type", SERVER_OPEN_SUCCESS);
            sendBroadcast(i);
        } catch (Exception e) {
            e.printStackTrace();
            Intent i = new Intent(getResources().getString(R.string.server_receiver_action));
            i.putExtra("type", SERVER_OPEN_FAILED);
            i.putExtra("message", e.getMessage());
            sendBroadcast(i);
            serverThread.close();
            serverThread = null;
            stopSelf();
        }
    }
}

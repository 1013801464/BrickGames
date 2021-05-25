package com.ykh.brickgames.games;

import android.os.Handler;
import android.os.Message;
import android.util.SparseIntArray;

import com.ykh.brickgames.Lg;
import com.ykh.brickgames.network.BrickNetBroadcast;
import com.ykh.brickgames.network.ClientThread;
import com.ykh.brickgames.network.ServerThread;
import com.ykh.brickgames.services.ClientService;

/**
 * Created by Ken on 2018/3/15.
 */

public class NetGame implements Control.Game {
    private Control control;
    private Handler inHandler;

    public NetGame(Control control) {
        this.control = control;
    }

    @Override
    public void onLeft() {
        if (inHandler != null) {
            Message m = new Message();
            m.what = ClientService.INSTRUCTION;
            m.arg1 = ServerThread.GO_LEFT;
            inHandler.sendMessage(m);
        }
    }

    @Override
    public void onRight() {
        if (inHandler != null) {
            Message m = new Message();
            m.what = ClientService.INSTRUCTION;
            m.arg1 = ServerThread.GO_RIGHT;
            inHandler.sendMessage(m);
        }
    }

    @Override
    public void onUp() {
        if (inHandler != null) {
            Message m = new Message();
            m.what = ClientService.INSTRUCTION;
            m.arg1 = ServerThread.GO_UP;
            inHandler.sendMessage(m);
        }
    }

    @Override
    public void onDown() {
        if (inHandler != null) {
            Message m = new Message();
            m.what = ClientService.INSTRUCTION;
            m.arg1 = ServerThread.GO_DOWN;
            inHandler.sendMessage(m);
        }
    }

    @Override
    public void onRotate() {
        if (inHandler != null) {
            Lg.e("客户端正在发出数据(游戏端)");
            Message m = new Message();
            m.what = ClientService.INSTRUCTION;
            m.arg1 = ServerThread.GO_FIRE;
            inHandler.sendMessage(m);
        }
    }

    @Override
    public void onStart(SparseIntArray Option) {
        BrickNetBroadcast.clientEnabled = true;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 20; j++) {
                control.screenData[i][j] = 0;
            }
        }
        for (int j = 8; j <= 12; j++) {
            control.screenData[2][j] = 10;
            control.screenData[3][j] = 10;
            control.screenData[6][j] = 10;
            control.screenData[7][j] = 10;
        }
        for (int j = 6; j <= 9; j++) {
            control.screenData[4][j] = 10;
            control.screenData[5][j] = 10;
        }
        BrickNetBroadcast.onClientBroadcastReceiver();
        try {
            inHandler = ClientThread.mThis.inHandler;
        } catch (Exception e) {
            inHandler = null;
        }
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onStop() {
        BrickNetBroadcast.clientEnabled = false;
    }

    @Override
    public int getScreenData(int x, int y) {
        if (BrickNetBroadcast.clientOk)
            return BrickNetBroadcast.screenData[x][y];
        return control.screenData[x][y];
    }
}

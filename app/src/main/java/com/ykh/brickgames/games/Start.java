package com.ykh.brickgames.games;

import android.os.Handler;
import android.os.Message;
import android.util.SparseIntArray;

import com.ykh.brickgames.Lg;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 开始界面
 */

class Start implements Control.Game {
    private final Control control;
    int xs[] = new int[4];          // 4个9的横坐标
    int ys[] = new int[4];          // 4个9的纵坐标
    private Handler handler;
    private Timer timer;
    private boolean enabled = true;
    private int[] queueX = new int[210];
    private int[] queueY = new int[210];
    private int count = 0;
    private Queue queue = new Queue();
    private int[][] data;
    private boolean visible = true;
    private boolean overturn = false;

    Start(final Control control) {
        this.control = control;
        for (int i = 0; i < 10; i++)
            Arrays.fill(control.screenData[i],0);
        data = new int[10][21];
        xs[0] = xs[2] = 6;      // 下面这六行加起来是一个图形: 9
        xs[1] = xs[3] = 1;
        ys[0] = 1;
        ys[1] = 5;
        ys[2] = 9;
        ys[3] = 13;
        InitHandler();
        count = 0;
        int start, end;
        for (int i = 0; i < 5; i++) {       // 计算环状的new point应该出现的位置
            start = i;
            end = 9 - i;
            for (int t = start; t <= end; t++) {
                queueX[count] = t;
                queueY[count++] = i;
            }
            start = i + 1;
            end = 18 - i;
            for (int t = start; t <= end; t++) {
                queueX[count] = 9 - i;
                queueY[count++] = t;
            }
            start = 9 - i;
            end = i;
            for (int t = start; t >= end; t--) {
                queueX[count] = t;
                queueY[count++] = 19 - i;
            }
            start = 18 - i;
            end = i + 1;
            for (int t = start; t >= end; t--) {
                queueX[count] = i;
                queueY[count++] = t;
            }
        }
        for (int i = 200; i < 210; i++) {
            queueX[i] = 0;
            queueY[i] = 20;
        }
    }

    @Override
    public void onStart(SparseIntArray Option) {
        Lg.e("开始屏幕 onStart()");
        enabled = true;
        count = 0;
        clear();            // 清除屏幕
        overturn = false;
        visible = true;
        queue.clear();
        onResume();         // 开一下计时器
    }

    private void InitHandler() {
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (enabled) {
                    if (count < 210) {
                        queue.put(queueX[count], queueY[count]);
                        if (overturn) MINUS();
                        else ADD();
                    } else if (count == 210) {
                        queue.clear();
                    } else if (count == 235) {
                        visible = false;        // 调整4个9的可见性
                    } else if (count == 260) {
                        visible = true;
                    } else if (count == 285) {
                        visible = false;
                    } else if (count == 310) {
                        visible = true;
                    } else if (count == 335) {
                        count = -1;
                        overturn = !overturn;
                    }
                    count++;
                    刷新();
                    control.invalidate();
                }
                return true;
            }
        });
    }

    private void ADD() {
        for (int i = 0; i < 10; i++) {
            data[queue.q1[i]][queue.q2[i]]++;
        }
    }

    private void MINUS() {
        for (int i = 0; i < 10; i++) {
            data[queue.q1[i]][queue.q2[i]]--;
        }
    }

    /**
     * 清除屏幕
     */
    private void clear() {
        for (int x = 0; x < 10; x++) {
            Arrays.fill(data[x], 0);
        }
    }

    private void 刷新() {
        for (int i = 0; i < 10; i++)
            System.arraycopy(data[i], 0, control.screenData[i], 0, 20);
        // 对4个9金进行反转
        for (int i = 0; i < 4 && visible; i++) {
            control.screenData[xs[i]][ys[i]] = 10 - control.screenData[xs[i]][ys[i]];
            control.screenData[xs[i] + 1][ys[i]] = 10 - control.screenData[xs[i] + 1][ys[i]];
            control.screenData[xs[i] + 2][ys[i]] = 10 - control.screenData[xs[i] + 2][ys[i]];
            control.screenData[xs[i]][ys[i] + 1] = 10 - control.screenData[xs[i]][ys[i] + 1];
            control.screenData[xs[i] + 2][ys[i] + 1] = 10 - control.screenData[xs[i] + 2][ys[i] + 1];
            control.screenData[xs[i]][ys[i] + 2] = 10 - control.screenData[xs[i]][ys[i] + 2];
            control.screenData[xs[i] + 1][ys[i] + 2] = 10 - control.screenData[xs[i] + 1][ys[i] + 2];
            control.screenData[xs[i] + 2][ys[i] + 2] = 10 - control.screenData[xs[i] + 2][ys[i] + 2];
            control.screenData[xs[i] + 2][ys[i] + 3] = 10 - control.screenData[xs[i] + 2][ys[i] + 3];
            control.screenData[xs[i]][ys[i] + 4] = 10 - control.screenData[xs[i]][ys[i] + 4];
            control.screenData[xs[i] + 1][ys[i] + 4] = 10 - control.screenData[xs[i] + 1][ys[i] + 4];
            control.screenData[xs[i] + 2][ys[i] + 4] = 10 - control.screenData[xs[i] + 2][ys[i] + 4];
        }
    }

    @Override
    public void onRotate() {
        stop();
    }

    @Override
    public void onLeft() {
        stop();
    }

    @Override
    public void onRight() {
        stop();
    }

    @Override
    public void onUp() {
        stop();
    }

    @Override
    public void onDown() {
        stop();
    }

    @Override
    public int getScreenData(int x, int y) {
        return control.screenData[x][y];
    }

    @Override
    public void onPause() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void onResume() {
        if (timer == null) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handler.sendEmptyMessage(0);
                }
            }, 0, 20);
        }
    }

    @Override
    public void onStop() {
        enabled = false;
        onPause();          // 关了计时器
    }

    public void stop() {
        onStop();
        control.endGame();
    }


    private class Queue {
        int[] q1;
        int[] q2;

        Queue() {
            q1 = new int[10];
            q2 = new int[10];
            clear();
        }

        /**
         * 放入一对坐标(将后面9个全部左移1位)
         */
        void put(int x, int y) {
            System.arraycopy(q1, 1, q1, 0, 9);
            System.arraycopy(q2, 1, q2, 0, 9);
            q1[9] = x;
            q2[9] = y;
        }

        void clear() {
            Arrays.fill(q1, 0);
            Arrays.fill(q2, 20);
        }
    }
}

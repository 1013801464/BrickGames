package com.ykh.brickgames.games;

import android.os.Handler;
import android.os.Message;
import android.util.SparseIntArray;

import com.ykh.brickgames.myViews.GameScreen;

import java.util.Arrays;
import java.util.Timer;

/**
 * 移动一个丄并发射子弹可以消除
 */

class MoveMinus implements Control.Game {
    private Control control;
    private int[][] data;           // 宽10, 高20
    private int mx;                 // 当前坦克坐标(以中心计, 即丄的左上)
    private Timer timer;
    private Timer timerb;
    private Handler handler;
    private boolean singleMode;     // 单个子弹模式
    private byte life;
    private int row1, row2;         // 显示打击的是哪一列/哪两列
    private int bullet[];
    private boolean prevent;
    private int scoreAtThisLevel;
    private GameScreen.Adapter adapter;

    MoveMinus(Control control) {
        this.control = control;
    }

    private void 生成一行并下移() {
        // 1. 元素下移
        for (int y = 19; y > 0; y--) {
            // 从第19行道第１行, 下移
            for (int x = 0; x < 10; x++) {
                // 只下移第0到第9列
                data[x][y] = data[x][y - 1];
            }
        }
        // 第1列到第10列随机生成数据
        for (int x = 0; x < 10; x++)
            data[x][0] = control.random.nextBoolean() ? 10 : 0;
    }

    private void createHandler() {
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == 0) {
                    生成一行并下移();
                    刷新();
                    if (失败了吗()) NewLevel(true, true);      // 如果游戏失败|开启新的一关
                } else {
                    刷新();
                }
                return false;
            }
        });
    }

    /**
     * 在游戏失败的时候调用
     *
     * @param delay         是否延迟半秒开始
     * @param showAnimation 是否显示动画
     */
    private void NewLevel(boolean delay, boolean showAnimation) {
        if (timer != null) timer.cancel();
        life--;
        if (life == 0) {
            if (timerb != null) {
                timerb.cancel();
                timerb = null;
            }
            control.endGame();              // 计时器已经关过了, 不管它了
            return;
        }
        control.SetPreviewScreen(control.lifeImage[life]);
        scoreAtThisLevel = 0;               // 本关得分重新计算
        if (showAnimation)
            control.startQuickShading();    // 开始动画
        for (int x = 0; x < 10; x++) {
            Arrays.fill(data[x], 0);        // 数据清零
        }
        mx = 5;
        timer = new Timer();
        timer.schedule(new TimerTask(), delay ? 300 : 0, getSpeed());       // 暂停为1秒钟下落一格
    }

    private void 刷新() {
        for (int i = 0; i < 10; i++)
            // 把图像的第1到10列复制到control.ScreenData的第0到第9列, 每一列20个数
            System.arraycopy(data[i], 0, control.screenData[i], 0, 20);
        // 添加小车
        control.screenData[mx][18] = 10;
        control.screenData[mx][19] = 10;
        if (mx == 0) {
            control.screenData[mx + 1][19] = 10;
        } else if (mx == 9) {
            control.screenData[mx - 1][19] = 10;
        } else {
            control.screenData[mx + 1][19] = 10;
            control.screenData[mx - 1][19] = 10;
        }
        if (row1 != -1) {
            for (int y = 17; y >= 0; y--) {
                if (control.screenData[row1][y] != 0) break;
                control.screenData[row1][y] = bullet[y];
            }
        }
        if (row2 != -1) {
            for (int y = 17; y >= 0; y--) {
                if (control.screenData[row2][y] != 0) break;
                control.screenData[row2][y] = bullet[y];
            }
        }
        control.invalidate();
    }

    private void 打击() {
        prevent = true;
        if (singleMode) {                       // 1列导弹模式
            row1 = mx;
            for (int i = 17; i >= 0; i--) {     // 从第17行开始倒着找
                if (data[row1][i] != 0) {
                    data[row1][i] = 0;
                    control.Add(Control.SCORE); // +1分
                    scoreAtThisLevel++;
                    break;
                }
            }
            if (scoreAtThisLevel >= 1000) {
                control.Add(Control.SPEED);
                scoreAtThisLevel = 0;
                timer.cancel();
                timer = new Timer();
                timer.schedule(new TimerTask(), 0, getSpeed());       // 暂停为1秒钟下落一格
            }
        } else {                                // 双列导弹模式
            if (mx == 9) {
                row1 = mx - 1;
                row2 = -1;
            } else {
                row1 = mx - 1;
                row2 = mx + 1;
            }
            if (row1 != -1)
                for (int i = 17; i >= 0; i--)      // 从第17行开始倒着找
                    if (data[row1][i] != 0) {
                        data[row1][i] = 0;
                        control.Add(Control.SCORE); // +1分
                        scoreAtThisLevel++;
                        break;
                    }
            if (row2 != -1)
                for (int i = 17; i >= 0; i--)      // 从第17行开始倒着找
                    if (data[row2][i] != 0) {
                        data[row2][i] = 0;
                        control.Add(Control.SCORE); // +1分
                        scoreAtThisLevel++;
                        break;
                    }
            if (scoreAtThisLevel >= 1000) {
                control.Add(Control.SPEED);
                scoreAtThisLevel = 0;
                timer.cancel();
                timer = new Timer();
                timer.schedule(new TimerTask(), 0, getSpeed());       // 暂停为1秒钟下落一格
            }
        }
        prevent = false;
        刷新();
    }

    private boolean 失败了吗() {
        for (int x = 0; x < 10; x++) {
            if (data[x][18] != 0) return true;
        }
        return false;
    }

    private long getSpeed() {
        int l = control.Get(Control.SPEED);
        return 2300 - 200 * l;
    }

    @Override
    public void onLeft() {
        if (mx > 0) mx--;
        刷新();
    }

    @Override
    public void onRight() {
        if (mx < 9) mx++;
        刷新();
    }

    @Override
    public void onUp() {

    }

    @Override
    public void onDown() {

    }

    @Override
    public void onRotate() {
        打击();
    }

    @Override
    public void onStart(SparseIntArray Option) {
        singleMode = Option.get(1) % 2 == 0;            // 相当于传入0和2都是单行模式
        for (int x = 0; x < 10; x++) Arrays.fill(control.screenData[x], 0);

        adapter = Option.get(1) / 2 % 2 == 0 ? new NormalAdapter() : new OverturnAdapter();
        life = 5;                   // 因为newlevel()里面会让life-1, 所以暂时设置成5
        data = new int[10][20];
        row1 = -1;
        row2 = -1;
        bullet = new int[18];       // 我需要0-17行
        bullet[17] = 8;
        bullet[16] = 8;
        bullet[15] = 8;
        bullet[14] = 8;
        bullet[13] = 7;
        bullet[12] = 6;
        bullet[11] = 5;
        bullet[10] = 4;
        bullet[9] = 3;
        bullet[8] = 2;
        bullet[7] = 1;
        createHandler();
        prevent = false;
        if (timerb == null) {
            timerb = new Timer();
            timerb.schedule(new ClearTimerTask(), 10, 20);       // 计划一秒执行50次
        }
        if (handler != null) createHandler();
        NewLevel(false, false);
    }

    @Override
    public void onPause() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timerb != null) {
            timerb.cancel();
            timerb = null;
        }
    }

    @Override
    public void onResume() {
        if (timer == null) {
            timer = new Timer();
            timer.schedule(new TimerTask(), getSpeed() / 2, getSpeed());
        }
        if (timerb == null) {
            timerb = new Timer();
            timerb.schedule(new ClearTimerTask(), 0, 20);
        }
    }

    @Override
    public void onStop() {
        onPause();
    }

    @Override
    public int getScreenData(int x, int y) {
        return adapter.getScreenData(x, y);
    }

    private class TimerTask extends java.util.TimerTask {
        @Override
        public void run() {
            handler.sendEmptyMessage(0);
        }
    }

    private class ClearTimerTask extends java.util.TimerTask {
        @Override
        public void run() {
            if (prevent) return;
            row1 = -1;
            row2 = -1;
            handler.sendEmptyMessage(1);
        }
    }

    private class NormalAdapter implements GameScreen.Adapter {
        @Override
        public int getScreenData(int x, int y) {
            return control.screenData[x][y];
        }
    }

    private class OverturnAdapter implements GameScreen.Adapter {
        @Override
        public int getScreenData(int x, int y) {
            return control.screenData[x][19 - y];
        }
    }
}

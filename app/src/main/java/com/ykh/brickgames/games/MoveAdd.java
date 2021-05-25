package com.ykh.brickgames.games;

import android.os.Handler;
import android.os.Message;
import android.util.SparseIntArray;

import com.ykh.brickgames.Lg;
import com.ykh.brickgames.myViews.GameScreen;

import java.util.Arrays;
import java.util.Timer;

/**
 * 移动一个丄并发射子弹可以加一个方块, 行满消除.
 */

class MoveAdd implements Control.Game {
    private Control control;
    private int[][] data;           // 宽10, 高21, 第一行用作哨兵
    private int mx;                 // 当前坦克坐标(以中心计, 即丄的左上)
    private int row1, row2;         // 显示打击的是哪一列/哪两列
    private int bullet[];
    private byte life;
    private int scoreAtThisLevel;
    private Timer timer;
    private Timer timerb;           // 负责子弹消失和行满动画
    private boolean prevent;
    private Handler handler;
    private boolean singleMode;     // 单个子弹模式
    private GameScreen.Adapter adapter;

    MoveAdd(Control control) {
        this.control = control;
    }

    private void 生成一行并下移() {
        // 1. 元素下移
        for (int y = 20; y > 1; y--) {
            // 从第20行道第2行, 下移
            for (int x = 0; x < 10; x++) {
                // 下移第0到第9列
                data[x][y] = data[x][y - 1];
            }
        }
        // 第1列到第10列随机生成数据
        for (int x = 0; x < 10; x++)
            data[x][1] = control.random.nextBoolean() ? 10 : 0;
    }

    private void 打击() {
        prevent = true;
        if (singleMode) {                       // 1列导弹模式
            row1 = mx;
            for (int i = 17; i >= 0; i--) {     // 从第17行开始倒着找
                if (data[row1][i] != 0) {
                    data[row1][i + 1] = 10;
                    break;
                }
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
                        data[row1][i + 1] = 10;
                        break;
                    }
            if (row2 != -1)
                for (int i = 17; i >= 0; i--)      // 从第17行开始倒着找
                    if (data[row2][i] != 0) {
                        data[row2][i + 1] = 10;
                        break;
                    }

        }
        处理行满();
        prevent = false;
        刷新();
    }

    private void 刷新() {
        for (int i = 0; i < 10; i++)
            // 把图像的第1到10列复制到control.ScreenData的第0到第9列, 每一列20个数
            System.arraycopy(data[i], 1, control.screenData[i], 0, 20);
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

    private long getSpeed() {
        int l = control.Get(Control.SPEED);
        Lg.e("返回速度" + (4600 - 3000 * l));
        return 4600 - 300 * l;
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
     * 输分两种情况
     * 第一种: 最后一行有东西
     * 第二种: 倒数第二行在车所在的一列有东西
     */
    private boolean 失败了吗() {
        for (int x = 0; x < 10; x++)
            if (data[x][20] != 0) return true;
        return data[mx][19] != 0;
    }

    private void 处理行满() {
        boolean flag;
        int x;
        for (int y = 1; y < 18; y++) {      // 只检查第1-18行, 第0行不能检查
            flag = true;
            for (x = 0; x < 10 && flag; x++)
                if (data[x][y] == 0) flag = false;
            if (flag) {                     // y便是行满的行号
                // 把下面的往上复制, 然后清除第20行
                for (int cx = 0; cx < 10; cx++) // 从第y+1行开始往上复制
                    System.arraycopy(data[cx], y + 1, data[cx], y, 20 - y);
                for (int cx = 0; cx < 10; cx++)
                    data[cx][20] = 0;
                control.Add(Control.SCORE); // +1分
                scoreAtThisLevel++;
                if (scoreAtThisLevel >= 40) {
                    timer.cancel();
                    timer = new Timer();
                    timer.schedule(new TimerTask(), 0, getSpeed());       // 暂停为1秒钟下落一格
                }
                break;      // 循环不用继续了, 因为最多同时一行满
            }
        }
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
            Arrays.fill(data[x], 1, 20, 0); // 不能对第0行清零
        }
        mx = 5;
        timer = new Timer();
        timer.schedule(new TimerTask(), delay ? 300 : 0, getSpeed());       // 暂停为1秒钟下落一格
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
        adapter = Option.get(1) / 2 % 2 == 0 ? new NormalAdapter() : new OverturnAdapter();
        life = 5;                   // 因为newlevel()里面会让life-1, 所以暂时设置成5
        data = new int[10][21];
        for (int x = 0; x < 10; x++) data[x][0] = 10;   // 第一行填充成10
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
        timerb = new Timer();
        timerb.schedule(new ClearTimerTask(), 10, 20);
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

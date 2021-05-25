package com.ykh.brickgames.games;

import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.util.SparseIntArray;

import com.ykh.brickgames.Lg;
import com.ykh.brickgames.myViews.GameScreen;

import java.util.ArrayList;
import java.util.Timer;

/**
 * 下面一个板进行弹跳的游戏.
 */

class SingleBounce implements Control.Game {
    private Control control;        // 控制器
    private short bx, by;           // 子弹横纵坐标
    private boolean right;          // 子弹方向, right = 1则向右, right = -1则向左
    private boolean down;           // 子弹方向, down = 1则向下, right = -1则向上
    private byte mx;                // 我的4个方格的板所在位置, 左坐标为准, 0-6
    private int[][] data;           // 图形数据, 包含现有图形, 宽12, 高22, 边界用作哨兵
    private GameScreen.Adapter adapter;     // 现用的图形是配器, 主要是为了将来翻转
    private Timer timer;
    private Timer moveTimer;        // 用来移动的计时器
    private boolean autoMove;       // 方块是否自动移动
    private Handler handler;        // 处理
    private byte life;              // 生命

    SingleBounce(Control control) {
        this.control = control;
    }

    /**
     * 将图形数据, 子弹 & 板放在一起, 交给Control
     */
    private void 刷新() {
        for (int x = 0; x < 10; x++)        // 把data中的相应部分复制到屏幕上
            System.arraycopy(data[x + 1], 1, control.screenData[x], 0, 20);
        for (int x = 0; x < 4; x++)         // 把板放上去
            control.screenData[mx + x][19] = 10;
        control.screenData[bx - 1][by - 1] = 6;     // 绘制子弹
        control.invalidate();
    }

    /**
     * 第一个参数代表是否延迟, 第二个参数代表是否对图形进行修复
     */
    private void NewLevel(boolean delay, boolean restore) {
        right = true;
        down = false;
        mx = 3;
        by = 20;
        bx = 5;
        if (restore)
            for (int y = 1; y < 6; y++) {                   // 对图形数据初始化
                for (int x = 1; x <= 10; x++) {
                    data[x][y] = control.random.nextBoolean() ? 10 : 0;
                }
            }
        control.SetPreviewScreen(control.lifeImage[life]);
        if (timer == null) {
            开启计时器(delay ? (byte) 1 : 0);
        }
    }

    /**
     * @param delay 2代表延迟一半, 0代表不延迟, 1代表延迟暂停的时间
     */
    private void 开启计时器(byte delay) {
        timer = new Timer();
        switch (delay) {
            case 2:
                timer.schedule(new TimerTask(), getSpeed() / 2, getSpeed());    // 暂时设置成1秒
                break;
            case 1:
                timer.schedule(new TimerTask(), 300, getSpeed());               // 暂时设置成1秒
                break;
            default:
                timer.schedule(new TimerTask(), 0, getSpeed());                 // 暂时设置成1秒
                break;
        }
        if (autoMove && moveTimer == null) {
            moveTimer = new Timer();
            moveTimer.schedule(new MoveTimerTask(),1000, 2000);                // 暂定为两秒移动一格
        }
    }

    /**
     * 到了一个位置, 先标记吃的, 然后寻找方向(可能要找好几遍), 然后吃. 不可能停下
     */
    private boolean 运动一格() {
        ArrayList<Point> points = new ArrayList<>();        // 存储要吃掉的点
        // 第一步: 走过去
        bx += right ? 1 : -1;
        by += down ? 1 : -1;
        // 第二步: 找方向, 顺便标记吃的
        int bxPlus, byPlus;
        bxPlus = bx + (right ? 1 : -1);
        byPlus = by + (down ? 1 : -1);
        // 第三步: 检查碰壁情况以及死亡情况
        if (data[bxPlus][by] == -1) right = !right;         // 水平方向翻转
        bxPlus = bx + (right ? 1 : -1);                     // 修改一下纠正后的坐标
        if (byPlus == 21) {
            失败处理();
            return false;
        }
        if (byPlus == 20) {                                 // 检查是否在最后一行, 因为有可能死亡
            if (bx == mx && bxPlus == mx + 1) {
                down = false;
                right = false;
            } else if (bx == mx + 5 && bxPlus == mx + 4) {
                down = false;
                right = true;
            } else if (bx >= mx + 1 && bx <= mx + 5) {
                down = false;
            } else {
                return true;
            }
        } else if (data[bx][byPlus] == -1) down = !down;    // 垂直方向偏转
        // 第四步: 沿着这个方向吃东西(只做标记), 顺便要校正方向
        bxPlus = bx + (right ? 1 : -1);
        byPlus = by + (down ? 1 : -1);
        if (data[bxPlus][by] == 0 && data[bx][byPlus] == 0) {
            if (data[bxPlus][byPlus] == 10) {
                points.add(new Point(bxPlus, byPlus));      // 添加要消除的点
                right = !right;                             // 修改方向
                down = !down;
            }
        } else {
            if (data[bxPlus][by] == 10) {
                points.add(new Point(bxPlus, by));
                right = !right;
            }
            if (data[bx][byPlus] == 10) {
                points.add(new Point(bx, byPlus));
                down = !down;
            }
        }
        // 第五步: 重新检查有没有撞墙
        bxPlus = bx + (right ? 1 : -1);                     // 重新计算坐标
        byPlus = by + (down ? 1 : -1);
        if (data[bxPlus][by] != 0) right = !right;          // 水平方向偏转
        if (data[bx][byPlus] != 0) down = !down;            // 垂直方向偏转
        // 第六步: 吃东西
        for (Point b : points) {
            data[b.x][b.y] = 0;                             // 吃掉这个点
            control.Add(Control.SCORE);
        }
        if (是否成功()) 成功处理();
        return true;
    }

    private void 图像右移() {
        int[] temp = new int[10];
        System.arraycopy(data[10], 1, temp, 0, 10);     // 备份第10列的10个方格
        for (int y = 10; y > 1; y--) {
            System.arraycopy(data[y - 1], 1, data[y], 1, 10);
        }
        System.arraycopy(temp, 0, data[1], 1, 10);      // 恢复第10列的方格
    }

    private void 失败处理() {
        刷新();
        control.startQuickShading();
        life--;
        if (life == 0) control.endGame();
        onPause();                          // 关闭计时器
        NewLevel(true, false);
    }

    /**
     * 默认返回不成功
     * 第1到10行, 如果有一个等于10, 即是方块, 则返回假, 否则返回真
     */
    private boolean 是否成功() {
        for (int y = 1; y <= 10; y++) {
            for (int x = 1; x <= 10; x++) {
                if (data[x][y] == 10) return false;
            }
        }
        return true;
    }

    private void 成功处理() {
        control.Add(Control.LEVEL);     // 增加1关
        control.startQuickShading();
        NewLevel(true, true);
    }

    private void createHandler() {
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (0 == msg.what) {
                    if (运动一格()) 刷新();
                } else if (1 == msg.what) {
                    图像右移();
                    刷新();
                }
                return true;
            }
        });
    }

    /**
     * 获得速度
     */
    private long getSpeed() {
        int l = control.Get(Control.LEVEL);
        return 820 - 70 * l;
    }

    @Override
    public void onLeft() {
        if (mx > 0) {
            mx--;
            if (by == 19 && bx >= mx && bx <= mx + 4) bx--;
        }
        刷新();
    }

    @Override
    public void onRight() {
        if (mx < 6) {
            mx++;
            if (by == 19 && bx >= mx && bx <= mx + 4) bx++;
        }
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
        运动一格();
        刷新();
    }

    @Override
    public void onStart(SparseIntArray Option) {
        data = new int[12][22];
        for (int i = 0; i < 12; i++) {
            data[i][0] = -1;                 // 把第一行设成边界-1
            data[i][21] = -2;                // 把第21行设成死亡区
        }
        for (int i = 1; i <= 20; i++) {
            data[0][i] = -1;                 // 把第0列设置成边界
            data[11][i] = -1;                // 把第11列设置成边界
        }
        life = 4;
        adapter = Option.get(1) / 2 % 2 == 0 ? new NormalAdapter() : new OverturnAdapter();
        autoMove = Option.get(1) % 2 == 0;
        Lg.e("option.get=" + Option.get(1) + " autoMove=" + autoMove);
        createHandler();
        NewLevel(false, true);
        刷新();
    }

    @Override
    public void onPause() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (moveTimer != null) {
            moveTimer.cancel();
            moveTimer = null;
        }
    }

    @Override
    public void onResume() {
        if (timer == null) {
            开启计时器((byte) 2);
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

    /**
     * 处理任务
     */
    private class TimerTask extends java.util.TimerTask {
        @Override
        public void run() {
            handler.sendEmptyMessage(0);
        }
    }

    /**
     * 用来定时移动的任务
     */
    private class MoveTimerTask extends java.util.TimerTask {
        @Override
        public void run() {
            Lg.e("成功发送了一则信息!");
            handler.sendEmptyMessage(1);
        }
    }
}

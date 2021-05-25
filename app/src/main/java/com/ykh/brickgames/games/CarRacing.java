package com.ykh.brickgames.games;

import android.os.Handler;
import android.os.Message;
import android.util.SparseIntArray;

import com.ykh.brickgames.Lg;
import com.ykh.brickgames.myViews.GameScreen;

import java.util.Arrays;
import java.util.Random;
import java.util.Timer;

/**
 * 极品飞车游戏
 * <p>
 * 设计:运动的赛车, 其基本形式是队列, 队列到屏幕的映射, 必须由两
 * 方面组成, 一是我自己, 二是赛车们. 我自己简单, 只有一个3×4的方
 * 格. 敌人状态描述可以由两个数组描写. left和right. left由20个
 * 数组成, 值为0-4, 分别是空和赛车的4节.
 */
class CarRacing implements Control.Game {
    private short[][] CAR;   // 特殊: 第一列是纵轴(0:3), 第二列是横轴(0:2)
    private short[] SIDE;
    private int[][] mScreen;        // 没有我的屏幕
    private Control control;
    private Timer timer;
    private Handler handler;
    private short flag;
    private int[] enemyLine;        // 3个二进制位 %2==?  /2%2==?  因为这个过程是不需要修改的
    private int myLocation;         // 0: 左, 1: 中, 2: 右(三列的话没有右)
    private boolean TwoLine = true; // 默认是两列的游戏
    private IntQueue queue;
    private short life;
    private short scoreAtThisLevel = 0;
    private GameScreen.Adapter adapter;


    CarRacing(Control control) {
        this.control = control;
    }

    // 初始化一些常量
    private void initConsts() {
        SIDE = new short[4];
        CAR = new short[4][3];
        SIDE[0] = 0;                            // 侧边
        SIDE[1] = SIDE[2] = SIDE[3] = 10;
        CAR[1][0] = CAR[1][1] = CAR[1][2] = 10;  // 汽车
        CAR[2][1] = CAR[0][1] = 10;
        CAR[3][0] = CAR[3][2] = 10;
    }

    @Override
    public void onLeft() {
        if (myLocation != 0) {
            myLocation--;
            是否在移动中输了();
        }
        刷新();
    }

    @Override
    public void onRight() {
        if (TwoLine && myLocation != 1 || !TwoLine && myLocation != 2) {
            myLocation++;
            是否在移动中输了();
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
        handler.sendEmptyMessage(0);
    }

    @Override
    public void onStart(SparseIntArray Option) {
        Lg.e("Car Racing : onStart()");
        TwoLine = Option.get(1) % 2 == 0;      // 0 2双行, 1 3单行
        adapter = Option.get(1) / 2 % 2 == 0 ? new NormalAdapter() : new OverturnAdapter();
        Lg.e("三行模式? " + !TwoLine + " Option=" + Option.get(1));
        mScreen = new int[10][20];
        initConsts();
        enemyLine = new int[20];
        flag = 0;
        life = 4;
        // 屏幕图像填充为0
        queue = new IntQueue();                     // 建造队列
        createHandler();
        NewLevel(true);
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
            timer.schedule(new TimerTask(), GetSpeed(control.Get(Control.SPEED)) / 2,
                    GetSpeed(control.Get(Control.SPEED)));
        }
    }

    @Override
    public void onStop() {
        if (timer != null) {        // 时钟不假
            timer.cancel();         // 在游戏结束中取消时钟
            timer = null;
        }
    }

    @Override
    public int getScreenData(int x, int y) {
        return adapter.getScreenData(x, y);
    }

    private void 是否在移动中输了() {
        // 判断是否输了: 这个位置有冲突
        boolean flag = true;
        // 首先求出待除数字
        int div = 1;
        if (myLocation == 1) div = 2;
        else if (myLocation == 2) div = 4;
        // 检查在myLocation的位置是否为1
        for (int y = 16; flag && y < 20; y++) {     // 第16 17 18 19列
            if (enemyLine[y] / div % 2 != 0) {
                flag = false;
            }
        }
        if (!flag) 生命减少();
    }

    private void 是否在下移中输了() {
        //检查第十六行: 在myLocation中是否有冲突
        int div = 1;
        if (myLocation == 1) div = 2;
        else if (myLocation == 2) div = 4;
        if (enemyLine[16] / div % 2 != 0) 生命减少();
    }

    private void 生命减少() {
        life--;
        if (life == 0) {
            onStop();
            control.endGame();
        } else {
            scoreAtThisLevel = 0;
            control.startQuickShading();
            NewLevel(false);
        }
    }

    /**
     * @param value 是否延时半秒
     */
    private void NewLevel(boolean value) {
        if (timer != null) {
            timer.cancel();         // 在新关卡中取消时钟
            timer = null;
        }
        myLocation = 1;             // 1: 中
        queue.size = 0;             // 清空队列
        queue.EnQueue();
        queue.EnQueue();
        control.SetPreviewScreen(control.lifeImage[life]);
        Arrays.fill(enemyLine, 0);  // 敌人队伍归0
        for (int i = 0; i < 10; i++) Arrays.fill(mScreen[i], 0);        // 清空屏幕(除0&9列)
        for (int y = 0; y < 20; y++) mScreen[0][y] = SIDE[flag++ % 4];
        if (TwoLine) System.arraycopy(mScreen[0], 0, mScreen[9], 0, 20);
        timer = new Timer();        // 新关卡需要计时(查找注)
        timer.schedule(new TimerTask(), value ? 0 : 300, GetSpeed(control.Get(Control.SPEED)));
    }

    private void 刷新() {
        for (int x = 0; x < 10; x++)
            System.arraycopy(mScreen[x], 0, control.screenData[x], 0, 20);
        if (TwoLine) {
            if (myLocation == 0) {      // 在左
                for (int x = 0; x < 3; x++)
                    for (int y = 0; y < 4; y++)
                        control.screenData[x + 2][y + 16] = CAR[y][x];
            } else {                    // 在右
                for (int x = 0; x < 3; x++)
                    for (int y = 0; y < 4; y++)
                        control.screenData[x + 5][y + 16] = CAR[y][x];
            }
        } else {
            if (myLocation == 0) {      // 在左
                for (int x = 0; x < 3; x++)
                    for (int y = 0; y < 4; y++)
                        control.screenData[x + 1][y + 16] = CAR[y][x];
            } else if (myLocation == 1) {                    // 在右
                for (int x = 0; x < 3; x++)
                    for (int y = 0; y < 4; y++)
                        control.screenData[x + 4][y + 16] = CAR[y][x];
            } else {
                for (int x = 0; x < 3; x++)
                    for (int y = 0; y < 4; y++)
                        control.screenData[x + 7][y + 16] = CAR[y][x];
            }
        }
        control.invalidate();
    }

    /**
     * 图像下移一格
     */
    private void 图像下移() {
        if (enemyLine[18] == 0 && enemyLine[19] != 0) {
            control.Add(Control.SCORE);
            scoreAtThisLevel++;
            if (scoreAtThisLevel >= 30) {
                scoreAtThisLevel = 0;
                control.Add(Control.SPEED);
                if (timer != null) {
                    timer.cancel();             // 新关卡需要取消时钟
                    timer = new Timer();        // 关卡增加需要重启计时器
                    timer.schedule(new TimerTask(), GetSpeed(control.Get(Control.SPEED)), GetSpeed(control.Get(Control.SPEED)));
                }
            }
        }
        for (short y = 19; y > 0; y--) {
            for (short x = 0; x < 10; x++)           // 复制第0, 2-7, 9行
                mScreen[x][y] = mScreen[x][y - 1];
            enemyLine[y] = enemyLine[y - 1];
        }
        mScreen[0][0] = SIDE[flag % 4];
        if (TwoLine) mScreen[9][0] = mScreen[0][0];
        // 第一行需要从队列里面取, td得到位置信息, tr得到余数信息
        int t = queue.DeQueue(), td = t / 8, tr = t % 8;
        enemyLine[0] = td;
        if (TwoLine) {              // 双列游戏
            if (td % 2 == 1) {      // t%2: 第一列, t/2%2: 第二列, t/4%2: 第三列
                mScreen[2][0] = CAR[tr - 1][0];
                mScreen[3][0] = CAR[tr - 1][1];
                mScreen[4][0] = CAR[tr - 1][2];
            } else                 // 否则通通赋值为0
                mScreen[2][0] = mScreen[3][0] = mScreen[4][0] = 0;
            if (td / 2 % 2 == 1) {
                mScreen[5][0] = CAR[tr - 1][0];
                mScreen[6][0] = CAR[tr - 1][1];
                mScreen[7][0] = CAR[tr - 1][2];
            } else                 // 否则通通赋值为0
                mScreen[5][0] = mScreen[6][0] = mScreen[7][0] = 0;
        } else {                    // !!!三列模式
            if (td % 2 == 1) {      // t%2: 第一列, t/2%2: 第二列, t/4%2: 第三列
                mScreen[1][0] = CAR[tr - 1][0];
                mScreen[2][0] = CAR[tr - 1][1];
                mScreen[3][0] = CAR[tr - 1][2];
            } else                 // 否则通通赋值为0
                mScreen[1][0] = mScreen[2][0] = mScreen[3][0] = 0;
            if (td / 2 % 2 == 1) {      // 第二列
                mScreen[4][0] = CAR[tr - 1][0];
                mScreen[5][0] = CAR[tr - 1][1];
                mScreen[6][0] = CAR[tr - 1][2];
            } else                 // 否则通通赋值为0
                mScreen[4][0] = mScreen[5][0] = mScreen[6][0] = 0;
            if (td / 4 % 2 == 1) {
                mScreen[7][0] = CAR[tr - 1][0];
                mScreen[8][0] = CAR[tr - 1][1];
                mScreen[9][0] = CAR[tr - 1][2];
            } else                 // 否则通通赋值为0
                mScreen[7][0] = mScreen[8][0] = mScreen[9][0] = 0;
        }
        是否在下移中输了();
    }

    private int GetSpeed(int S) {
        return 350 - 30 * S;
    }

    private void createHandler() {
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                flag--;
                if (flag <= 0) flag = 16;
                图像下移();
                刷新();
                return false;
            }
        });
    }

    /**
     * 存放int型的顺序队列
     */
    private class IntQueue {
        int size;               // 当前已有数量
        int content[];          // 最错存储20个数据

        IntQueue() {
            content = new int[20];
            size = 0;
        }

        void EnQueue() {
            if (size <= 10) {       // 保证充足的容量(10个数)
                int t;              // 我最终需要一个数据解析器, 解析出每一列的01234(事后解析)
                if (TwoLine) {      // 乘8赋予+01234, 以后%8即得1234, /8即得左右, 多好!
                    t = control.random.nextInt(2) + 1;
                } else {
                    t = control.random.nextInt(6) + 1;
                }
                t *= 8;
                for (int i = 4; i > 0; i--) {       // 4个数: 4->1
                    content[size++] = t + i;
                }
                for (int i = 0; i < 5; i++) {       // 5个数: 0
                    content[size++] = 0;            // 没有乘t是因为没有敌人
                }
            }
        }

        int DeQueue() {
            if (size > 0) {
                int temp = content[0];
                for (int i = 1; i < size; i++) {
                    content[i - 1] = content[i];    // 把数朝前复制
                }
                size--;
                return temp;
            }
            if (size <= 10) EnQueue();
            return 0;
        }
        // 队头进, 队尾出 φ(゜▽゜*)♪
    }

    class TimerTask extends java.util.TimerTask {
        @Override
        public void run() {
            handler.sendEmptyMessage(0);
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

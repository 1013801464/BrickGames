package com.ykh.brickgames.games;

import android.os.Handler;
import android.os.Message;
import android.util.SparseIntArray;

import com.ykh.brickgames.Lg;
import com.ykh.brickgames.network.ServerThread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 联网的坦克游戏
 */
public class NetTankGame implements Control.Game {
    private Control control;
    private int[][] screenData;             // 屏幕数据, 这是合成之后的结果
    private int[][] tanksScreenData;        // 坦克显示数据(0~10第一只坦克, 11~21第二只坦克, 22~32, 33~43, 44~54我的坦克)
    private int[][] bulletScreenData;       // 子弹显示数据
    private int[][] treeScreenData;         // 森林显示数据
    private Tank[] tanks;                   // tank[0-3]代表敌人
    private Tank mTank;                    // 自己的坦克
    private ArrayList<Bullet> bullets;      // 子弹
    private Handler handler;
    private Timer bulletTimer;              // 负责子弹运动的计时器
    private byte life = 4;                  // 生命数量
    private int speed;                      // 速度
    private int lastEnemyTank = 0;

    /**
     * 构造函数
     */
    NetTankGame(Control control) {
        this.control = control;
        screenData = new int[10][20];
        tanks = new Tank[3];
        tanks[0] = new Tank(Tank.RIGHT, 1, 10, 2);
        tanks[1] = new Tank(Tank.RIGHT, 1, 1, 3);
        tanks[2] = new Tank(Tank.LEFT, 8, 1, 4);
        mTank = new Tank(Tank.UP, 4, 9, 1);
        tanksScreenData = new int[10][20];                      // 5只坦克合并的图像数据
        bulletScreenData = new int[10][20];
        bullets = new ArrayList<>(10);                          // 分配10个子弹空间(不够了自己加吧)
    }

    @Override
    public void onStart(SparseIntArray Option) {
        Lg.e("网络坦克游戏 onStart()");
        speed = Option.get(2);
        // 获得服务器线程的事件处理器
        ServerThread.handler = new TankGameHandler(this);
        // 大量数据初始化操作
        treeScreenData = new int[10][20];                       // 丛林
        读取森林数据();
        for (int i = 0; i < 3; i++)
            tanks[i].visible = false;
        life = 4;
        // 创建Handler
        createHandlers();
        NewLevel(false, false);
        // 刷新图像
        刷新坦克图像();
        刷新屏幕();
    }

    private void 读取森林数据() {
        treeScreenData[1][3] = 65;                              // 丛林赋值
        treeScreenData[1][4] = 65;
        treeScreenData[2][3] = 65;
        treeScreenData[8][4] = 65;
        treeScreenData[8][3] = 65;
        treeScreenData[7][3] = 65;
        treeScreenData[1][16] = 65;
        treeScreenData[1][15] = 65;
        treeScreenData[2][16] = 65;
    }

    @Override
    public void onStop() {
        Lg.e("网络坦克游戏的onStop方法执行!");
        // 取消时钟
        if (bulletTimer != null)
            bulletTimer.cancel();
        bulletTimer = null;
        // 清除处理器
        handler = null;
    }

    @Override
    public void onRotate() {
        if (mTank.visible) {
            bullets.add(new Bullet(mTank.getBulletX(), mTank.getBulletY(), mTank.dire, 1));
            control.playSound(1);
        }
    }

    @Override
    public void onLeft() {
        mTank.goLeft();
        刷新坦克图像();
        刷新屏幕();
    }

    @Override
    public void onRight() {
        mTank.goRight();
        刷新坦克图像();
        刷新屏幕();
    }

    @Override
    public void onUp() {
        mTank.goUp();
        刷新坦克图像();
        刷新屏幕();
    }

    @Override
    public void onDown() {
        mTank.goDown();
        刷新坦克图像();
        刷新屏幕();
    }

    @Override
    public void onPause() {
        if (bulletTimer != null) {
            bulletTimer.cancel();
            bulletTimer = null;
        }
    }

    @Override
    public void onResume() {
        if (bulletTimer == null) {
            bulletTimer = new Timer();
            bulletTimer.schedule(new BulletTimerTask(), 20, 100);
        }
    }

    @Override
    public int getScreenData(int x, int y) {
        return screenData[x][y];
    }

    /**
     * 刷新坦克和丛林图像(只刷新数据, 不invalidate)
     */
    private void 刷新坦克图像() {
        // 清空所有坦克图像
        for (int x = 0; x < 10; x++) {
            System.arraycopy(treeScreenData[x], 0, tanksScreenData[x], 0, 20);
        }
        int cx = mTank.x, cy = mTank.y;
        if (mTank.visible) {
            tanksScreenData[cx - 1][cy - 1] = max_11(tanksScreenData[cx - 1][cy - 1], mTank.getImage(0) + 44);
            tanksScreenData[cx][cy - 1] = max_11(tanksScreenData[cx][cy - 1], mTank.getImage(1) + 44);
            tanksScreenData[cx + 1][cy - 1] = max_11(tanksScreenData[cx + 1][cy - 1], mTank.getImage(2) + 44);
            tanksScreenData[cx - 1][cy] = max_11(tanksScreenData[cx - 1][cy], mTank.getImage(3) + 44);
            tanksScreenData[cx][cy] = max_11(tanksScreenData[cx][cy], mTank.getImage(4) + 44);
            tanksScreenData[cx + 1][cy] = max_11(tanksScreenData[cx + 1][cy], mTank.getImage(5) + 44);
            tanksScreenData[cx - 1][cy + 1] = max_11(tanksScreenData[cx - 1][cy + 1], mTank.getImage(6) + 44);
            tanksScreenData[cx][cy + 1] = max_11(tanksScreenData[cx][cy + 1], mTank.getImage(7) + 44);
            tanksScreenData[cx + 1][cy + 1] = max_11(tanksScreenData[cx + 1][cy + 1], mTank.getImage(8) + 44);
        }
        for (int i = 0; i < 3; i++) {               // 为了避免重叠需要本坦克与旧图像数据取最大值
            cx = tanks[i].x;
            cy = tanks[i].y;                        // 日后只需要对11整除即可得到是哪一辆坦克
            tanksScreenData[cx - 1][cy - 1] = max_11(tanksScreenData[cx - 1][cy - 1], tanks[i].getImage(0) + 11 * i);
            tanksScreenData[cx][cy - 1] = max_11(tanksScreenData[cx][cy - 1], tanks[i].getImage(1) + 11 * i);
            tanksScreenData[cx + 1][cy - 1] = max_11(tanksScreenData[cx + 1][cy - 1], tanks[i].getImage(2) + 11 * i);
            tanksScreenData[cx - 1][cy] = max_11(tanksScreenData[cx - 1][cy], tanks[i].getImage(3) + 11 * i);
            tanksScreenData[cx][cy] = max_11(tanksScreenData[cx][cy], tanks[i].getImage(4) + 11 * i);
            tanksScreenData[cx + 1][cy] = max_11(tanksScreenData[cx + 1][cy], tanks[i].getImage(5) + 11 * i);
            tanksScreenData[cx - 1][cy + 1] = max_11(tanksScreenData[cx - 1][cy + 1], tanks[i].getImage(6) + 11 * i);
            tanksScreenData[cx][cy + 1] = max_11(tanksScreenData[cx][cy + 1], tanks[i].getImage(7) + 11 * i);
            tanksScreenData[cx + 1][cy + 1] = max_11(tanksScreenData[cx + 1][cy + 1], tanks[i].getImage(8) + 11 * i);
        }
    }

    private void createHandlers() {
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) { // 本消息负责让所有子弹前进
                if (0 == msg.what && !bullets.isEmpty()) {
                    /*子弹事件*/
                    Bullet b;
                    int flagBullet;                  // 标记 (-1:移除子弹  )
                    short failLocation = 0;
                    for (int i = 0; i < bullets.size() && failLocation < 2; ++i) {   // ABC < 2保证游戏没有失败
                        flagBullet = 0;              // 首先对b重置
                        b = bullets.get(i);
                        if (!b.go()) {               // 子弹正常行走,但是越界了
                            flagBullet = -1;         // 做移除子弹的标记
                        } else if (tanksScreenData[b.x][b.y] != 0) {     // 如果子弹是遇到坦克们了
//                          failLocation = (short) (b.x + b.y * 10 + 3); // 记录失败坐标
//                          control.Add(Control.SCORE); // 得分+1
//                          if (tankCount == 0) failLocation = 2;        // 游戏胜利标记
//                           else failLocation = 1;                      // 做需要刷新坦克图像的标记
                            // 判断子弹和坦克是不是同一个人
                            // 子弹ID:坦克ID 2:0, 3:1, 4:2, 1:4
                            int tid = tanksScreenData[b.x][b.y] / 11;
                            if (b.id == 2 && tid == 0 || b.id == 3 && tid == 1 || b.id == 4 && tid == 2 || b.id == 1 && tid == 4) {
                                // 如果子弹的发送者和遇到的坦克是同一个人, 则让子弹消失(应该不会出现)
                                flagBullet = -1;
                            } else if (tid < 4) {  // 如果不是同一个人 至少保证这是一只坦克
                                if (tanks[tid].visible) {
                                    tanks[tid].visible = false;
                                    flagBullet = -1;
                                }
                            } else if (tid == 4) {
                                if (mTank.visible) {
                                    mTank.visible = false;
                                    flagBullet = -1;
                                }
                            } else if (tanksScreenData[b.x][b.y] == 65) {
                                treeScreenData[b.x][b.y] = 55;               // 丛林消失
                                failLocation = 1;                            // 做需要刷新坦克的标记
                                flagBullet = -1;
                            }
                        } else if (bulletScreenData[b.x][b.y] != 0) {   // 如果遇到一个子弹[与上一时刻的对比]
                            flagBullet = bullets.size();
                            for (int j = 0; j < flagBullet; j++) {
                                if (i != j && bullets.get(j).x == b.x && bullets.get(j).y == b.y) {
                                    bullets.remove(j);
                                    flagBullet = -1;
                                    if (j < i) --i;
                                    break;
                                }
                            }
                        }
                        if (flagBullet == -1) {
                            bullets.remove(b);
                            --i;
                        }
                        for (int x = 0; x < 10; x++) Arrays.fill(bulletScreenData[x], 0); // 清空数据
                        for (Bullet c : bullets) bulletScreenData[c.x][c.y] = 5;          // 填上子弹
                    } // for - bullets
                    if (failLocation == 1)
                        刷新坦克图像();
                    if (failLocation == 2) {
                        // 清除屏幕上所有子弹遗迹
                        for (int x = 0; x < 10; x++) Arrays.fill(bulletScreenData[x], 0);
                        游戏胜利();
                    } else if (failLocation >= 3) {
                        // 清除屏幕上所有子弹遗迹
                        for (int x = 0; x < 10; x++) Arrays.fill(bulletScreenData[x], 0);
                        failLocation -= 3;
                        开始失败动画(failLocation % 10, failLocation / 10);
                    } else
                        刷新屏幕();
                }
                return true;
            }
        });
    }

    private Tank nextVisibleTank() {
        for (int i = 1; i <= 4; i++) {
            if (tanks[(lastEnemyTank + i) % 4].visible) {
                lastEnemyTank = (lastEnemyTank + i) % 4;
                return tanks[lastEnemyTank];
            }
        }
        return null;
    }

    private void 发子弹(Tank t) {
        bullets.add(new Bullet(t.getBulletX(), t.getBulletY(), t.dire, t.id));
    }

    /**
     * 新关卡, 参数是是否让关卡+1, 第二个参数表示是否延迟
     */
    private void NewLevel(boolean NEW, boolean delay) {
        if (NEW) control.Add(Control.LEVEL);        // 关卡加一
        bullets.clear();
        // 初始化我的坦克的位置, 以及把敌人坦克设置为隐藏
        for (int i = 0; i < 3; i++)
            tanks[i].visible = true;
        tanks[0].x = 8;
        tanks[0].y = 1;
        tanks[0].dire = Tank.UP;

        tanks[1].x = 1;
        tanks[1].y = 18;
        tanks[1].dire = Tank.DOWN;

        tanks[2].x = 8;
        tanks[2].y = 18;
        tanks[2].dire = Tank.DOWN;

        mTank.dire = Tank.UP;
        mTank.x = 1;
        mTank.y = 1;
        control.SetPreviewScreen(control.lifeImage[life]);
        读取森林数据();
        // 启动计时器
        bulletTimer = new Timer();
        bulletTimer.schedule(new BulletTimerTask(), delay ? 300 : 0, 100);     // 每40毫秒子弹前进一格
    }

    private long GetTime(int speed) {
        return 407L - 30 * speed;
    }

    private void 开始失败动画(int x, int y) {
        Lg.e("失败于 x=" + x + " y=" + y);
        bulletTimer.cancel();
        bulletTimer = null;
        life--;
        if (life <= 0) {
            onStop();
            control.endGame();
        } else {
            control.startQuickShading();
            NewLevel(false, true);
        }
    }

    private void 游戏胜利() {
        // 取消所有计时器
        bulletTimer.cancel();
        bulletTimer = null;
        control.startQuickShading();
        NewLevel(true, true);
    }

    private void 刷新屏幕() {
        for (int x = 0; x < 10; x++)
            for (int y = 0; y < 20; y++)
                screenData[x][y] = Math.max(tanksScreenData[x][y] % 11, bulletScreenData[x][y]);
        control.invalidate();
    }

    private int max_11(int a, int b) {
        if (a % 11 > b % 11) return a;
        return b;
    }

    private static class TankGameHandler extends Handler {
        NetTankGame game;

        TankGameHandler(NetTankGame game) {
            super();
            this.game = game;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ServerThread.INSTRUCTIONS_FROM_SERVER) {
                Lg.e("服务器端收到了从客户端发来的指令");
                Tank dest = game.tanks[msg.arg1 - 2];       // 要操作的目标坦克(由 2 3 4 映射到0 1 2所以减2)
                switch (msg.arg2) {
                    case ServerThread.GO_LEFT:
                        dest.goLeft();
                        break;
                    case ServerThread.GO_RIGHT:
                        dest.goRight();
                        break;
                    case ServerThread.GO_UP:
                        dest.goUp();
                        break;
                    case ServerThread.GO_DOWN:
                        dest.goDown();
                        break;
                    case ServerThread.GO_FIRE:
                        if (dest.visible)
                            game.发子弹(dest);
                        break;
                }
                game.刷新坦克图像();
                game.刷新屏幕();
            }
        }
    }

    /**
     * 坦克类型(敌人的坦克)
     */
    private class Tank {
        final static byte LEFT = 1;
        final static byte RIGHT = 2;
        final static byte UP = 3;
        final static byte DOWN = 4;
        int last = 9;   // 记录上次的方向
        int x;          // X坐标
        int y;          // Y坐标
        byte dire;      // 方向
        boolean visible;
        int id;         // 坦克ID

        Tank(byte dire, int x, int y, int id) {
            this.dire = dire;
            this.x = x;
            this.y = y;
            //todo recouver// this.visible = false;
            this.visible = true;
            this.id = id;
        }

        /**
         * 图像按行扫描, 0-2第一行, 3-5第二行, 6-8第三行
         */
        int getImage(int i) {
            switch (i) {
                case 0:
                    return visible && (dire == RIGHT || dire == DOWN) ? 10 : 0;
                case 1:
                    return visible && dire != DOWN ? 10 : 0;
                case 2:
                    return visible && (dire == LEFT || dire == DOWN) ? 10 : 0;
                case 3:
                    return visible && dire != RIGHT ? 10 : 0;
                case 4:
                    return visible ? 10 : 0;
                case 5:
                    return visible && dire != LEFT ? 10 : 0;
                case 6:
                    return visible && (dire == RIGHT || dire == UP) ? 10 : 0;
                case 7:
                    return visible && dire != UP ? 10 : 0;
                case 8:
                    return visible && (dire == UP || dire == LEFT) ? 10 : 0;
                default:            // 遇到意外返回0
                    return 0;
            }

        }

        /**
         * 坦克往下走
         */
        boolean goDown() {
            if (visible) {
                if (dire == DOWN) {
                    if (y < 18) {
                        y++;
                        if (is有冲突()) {
                            y--;
                            return false;       // 产生冲突导致运动失败
                        }
                    } else return false;        // 坐标y太大导致运动失败
                } else/*dire!=DOWN*/ {
                    byte last = dire;
                    dire = DOWN;
                    if (is有冲突()) {
                        if (!goDown()) {
                            dire = last;
                            return false;       // 旋转失败, 运动失败, 彻底失败
                        }
                    }
                }
                return true;
            }
            return false;                       // 坦克不可见导致运动失败
        }

        /**
         * 坦克往上走
         */
        boolean goUp() {
            if (visible) {
                if (dire == UP) {
                    if (y > 1) {
                        y--;
                        if (is有冲突()) {
                            y++;
                            return false;       // 产生冲突导致运动失败
                        }
                    } else return false;        // 坐标y太小导致运动失败
                } else {
                    byte last = dire;
                    dire = UP;
                    if (is有冲突()) {
                        if (!goUp()) {
                            dire = last;
                            return false;       // 旋转失败, 运动失败, 彻底失败
                        }
                    }
                }
                return true;
            }
            return false;                       // 坦克不可见 导致运动失败
        }

        /**
         * 坦克向右走
         */
        boolean goRight() {
            if (visible) {
                if (dire == RIGHT) {
                    if (x < 8) {
                        x++;
                        if (is有冲突()) {
                            x--;
                            return false;       // 有冲突导致失败
                        }
                    } else return false;        // 坐标x过大导致失败
                } else {
                    byte last = dire;
                    dire = RIGHT;
                    if (is有冲突()) {
                        if (!goRight()) {
                            dire = last;
                            return false;
                        }
                    }
                }
                return true;
            }
            return false;                       // 坦克不可见导致失败
        }

        /**
         * 坦克向左走
         */
        boolean goLeft() {
            if (visible) {      // 如果坦克是活的;
                if (dire == LEFT) {
                    if (x > 1) {
                        x--;
                        if (is有冲突()) {
                            x++;
                            return false;
                        }           // 撤销修改
                    } else return false;
                } else {
                    byte last = dire;
                    dire = LEFT;
                    if (is有冲突()) {
                        if (!goLeft()) {
                            dire = last;
                            return false;
                        }
                    }
                }
                return true;
            }
            return false;
        }

        /**
         * 返回是否没有冲突(一定要在可见的情况下检查!)
         */
        private boolean is有冲突() {
            visible = false;                // 设为不可见
            刷新坦克图像();
            visible = true;
            for (int d = 0; d < 9; d++) {   // 冲突情况是 我有方块, 坦克也有了方块
                if (getImage(d) != 0
                        && tanksScreenData[x + d % 3 - 1][y + d / 3 - 1] % 11 != 0) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 获得发射子弹的坐标 X
         */
        int getBulletX() {
            if (dire == UP || dire == DOWN) return x;
            else if (dire == LEFT) return x - 1;
            else return x + 1;
        }

        /**
         * 获得发射子弹的坐标 Y
         */
        int getBulletY() {
            if (dire == LEFT || dire == RIGHT) return y;
            else if (dire == UP) return y - 1;
            else return y + 1;
        }
    }


    /**
     * 子弹
     */
    private class Bullet {
        int x, y;        // 坐标
        byte dire;       // 方向 0左 1上 2右 3下
        int id;    // 是不是我发射的子弹

        /**
         * @param x    坐标X
         * @param y    坐标Y
         * @param dire 方向
         * @param id   是谁发的雷
         */
        Bullet(int x, int y, byte dire, int id) {
            this.x = x;
            this.y = y;
            this.dire = dire;
            this.id = id;
        }

        /**
         * 走一步
         *
         * @return 运动是否成功
         */
        boolean go() {
            if (dire == 1) return goLeft();
            else if (dire == 3) return goUp();
            else if (dire == 2) return goRight();
            else return goDown();
        }

        /**
         * 向左行走
         *
         * @return 操作是否成功
         */
        private boolean goLeft() {
            if (x <= 0) return false;
            else x--;
            return true;
        }

        private boolean goRight() {
            if (x >= 9) return false;
            else x++;
            return true;
        }

        private boolean goDown() {
            if (y >= 19) return false;
            else y++;
            return true;
        }

        private boolean goUp() {
            if (y <= 0) return false;
            else y--;
            return true;
        }
    }

    /**
     * 子弹的计时器
     */
    private class BulletTimerTask extends TimerTask {
        @Override
        public void run() {
            handler.sendEmptyMessage(0);
        }
    }
}

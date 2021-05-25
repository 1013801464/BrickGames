package com.ykh.brickgames.games;

import android.os.Handler;
import android.os.Message;
import android.util.SparseIntArray;

import com.ykh.brickgames.Lg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 坦克游戏.
 */

class TankGame implements Control.Game {
    private Control control;
    private int[][] screenData;             // 屏幕数据, 这是合成之后的结果
    private int[][] tanksScreenData;        // 坦克显示数据
    private int[][] bulletScreenData;       // 子弹显示数据
    private int[][] treeScreenData;         // 森林显示数据
    private Tank[] tanks;                   // tank[0-3]代表敌人
    private MTank mTank;                    // 自己的坦克
    private ArrayList<Bullet> bullets;      // 子弹
    private Handler handler;
    private Timer bulletTimer;              // 负责子弹运动的计时器
    private Timer enemyTimer;               // 负责操作敌人的计时器
    private int tankCount = 0;              // 剩余坦克计数
    private byte life = 4;                  // 生命数量
    private int speed;                      // 速度
    private int lastEnemyTank = 0;

    /*构造函数*/
    TankGame(Control control) {
        this.control = control;
        screenData = new int[10][20];
        tanks = new Tank[4];
        tanks[0] = new Tank(Tank.RIGHT, 1, 10);
        tanks[1] = new Tank(Tank.RIGHT, 1, 1);
        tanks[2] = new Tank(Tank.LEFT, 8, 1);
        tanks[3] = new Tank(Tank.UP, 8, 18);
        mTank = new MTank(Tank.UP, 4, 9);
        tanksScreenData = new int[10][20];                      // 5只坦克合并的图像数据
        bulletScreenData = new int[10][20];
        bullets = new ArrayList<>(10);                          // 分配10个子弹空间(不够了自己加吧)
    }

    @Override
    public void onStart(SparseIntArray Option) {
        Lg.e("坦克游戏 onStart()");
        speed = Option.get(2);
        // 大量数据初始化操作
        treeScreenData = new int[10][20];                       // 丛林
        读取森林数据();
        for (int i = 0; i < 4; i++)
            tanks[i].visible = false;
        life = 4;
        // 创建Handler
        createHandlers();
        NewLevel(false, false);
        // 刷新图像
        刷新坦克图像();
        刷新();
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
        Lg.e("坦克游戏的onStop方法执行!");
        // 取消时钟
        if (bulletTimer != null)
            bulletTimer.cancel();
        if (enemyTimer != null)
            enemyTimer.cancel();
        bulletTimer = null;
        enemyTimer = null;
        // 清除处理器
        handler = null;
        // 清除坦克
        // 清除屏幕数据
    }

    @Override
    public void onRotate() {
        bullets.add(new Bullet(mTank.getBulletX(), mTank.getBulletY(), mTank.dire, true));
        control.playSound(1);
    }

    @Override
    public void onLeft() {
        mTank.goLeft();
        刷新坦克图像();
        刷新();
    }

    @Override
    public void onRight() {
        mTank.goRight();
        刷新坦克图像();
        刷新();
    }

    @Override
    public void onUp() {
        mTank.goUp();
        刷新坦克图像();
        刷新();
    }

    @Override
    public void onDown() {
        mTank.goDown();
        刷新坦克图像();
        刷新();
    }

    @Override
    public void onPause() {
        if (bulletTimer != null) {
            bulletTimer.cancel();
            bulletTimer = null;
        }
        if (enemyTimer != null) {
            enemyTimer.cancel();
            enemyTimer = null;
        }
    }

    @Override
    public void onResume() {
        if (bulletTimer == null) {
            bulletTimer = new Timer();
            bulletTimer.schedule(new BulletTimerTask(), 20, 100);
        }
        if (enemyTimer == null) {
            enemyTimer = new Timer();
            enemyTimer.schedule(new EnemyTimerTask(), GetTime(speed) / 2L, GetTime(speed));
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
            tanksScreenData[cx - 1][cy - 1] =
                    max_11(tanksScreenData[cx - 1][cy - 1], mTank.getImage(0) + 44);
            tanksScreenData[cx][cy - 1] =
                    max_11(tanksScreenData[cx][cy - 1], mTank.getImage(1) + 44);
            tanksScreenData[cx + 1][cy - 1] =
                    max_11(tanksScreenData[cx + 1][cy - 1], mTank.getImage(2) + 44);
            tanksScreenData[cx - 1][cy] =
                    max_11(tanksScreenData[cx - 1][cy], mTank.getImage(3) + 44);
            tanksScreenData[cx][cy] =
                    max_11(tanksScreenData[cx][cy], mTank.getImage(4) + 44);
            tanksScreenData[cx + 1][cy] =
                    max_11(tanksScreenData[cx + 1][cy], mTank.getImage(5) + 44);
            tanksScreenData[cx - 1][cy + 1] =
                    max_11(tanksScreenData[cx - 1][cy + 1], mTank.getImage(6) + 44);
            tanksScreenData[cx][cy + 1] =
                    max_11(tanksScreenData[cx][cy + 1], mTank.getImage(7) + 44);
            tanksScreenData[cx + 1][cy + 1] =
                    max_11(tanksScreenData[cx + 1][cy + 1], mTank.getImage(8) + 44);
        }
        for (int i = 0; i < 4; i++) {               // 为了避免重叠需要本坦克与旧图像数据取最大值
            cx = tanks[i].x;
            cy = tanks[i].y;                        // 日后只需要对11整除即可得到是哪一辆坦克
            tanksScreenData[cx - 1][cy - 1] =
                    max_11(tanksScreenData[cx - 1][cy - 1], tanks[i].getImage(0) + 11 * i);
            tanksScreenData[cx][cy - 1] =
                    max_11(tanksScreenData[cx][cy - 1], tanks[i].getImage(1) + 11 * i);
            tanksScreenData[cx + 1][cy - 1] =
                    max_11(tanksScreenData[cx + 1][cy - 1], tanks[i].getImage(2) + 11 * i);
            tanksScreenData[cx - 1][cy] =
                    max_11(tanksScreenData[cx - 1][cy], tanks[i].getImage(3) + 11 * i);
            tanksScreenData[cx][cy] =
                    max_11(tanksScreenData[cx][cy], tanks[i].getImage(4) + 11 * i);
            tanksScreenData[cx + 1][cy] =
                    max_11(tanksScreenData[cx + 1][cy], tanks[i].getImage(5) + 11 * i);
            tanksScreenData[cx - 1][cy + 1] =
                    max_11(tanksScreenData[cx - 1][cy + 1], tanks[i].getImage(6) + 11 * i);
            tanksScreenData[cx][cy + 1] =
                    max_11(tanksScreenData[cx][cy + 1], tanks[i].getImage(7) + 11 * i);
            tanksScreenData[cx + 1][cy + 1] =
                    max_11(tanksScreenData[cx + 1][cy + 1], tanks[i].getImage(8) + 11 * i);
        }
        // 把坦克扩大到周围5*5就可以抹去剩余位置 or 根据方向修复周围3个方块; 怎样保证是历史遗迹而不是丛林?
    }

    private void createHandlers() {
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) { // 本消息负责让所有子弹前进
                if (0 == msg.what && !bullets.isEmpty()) {
                    /*子弹事件*/
                    Bullet b;
                    int t;
                    short ABC = 0;
                    for (int i = 0; i < bullets.size() && ABC < 2; ++i) {   // ABC<2保证游戏没有失败
                        t = 0;              // 首先对b重置
                        b = bullets.get(i);
                        if (!b.go()) {      // 子弹正常行走,但是越界了
                            t = -1;         // 做移除子弹的标记
                        } else if (tanksScreenData[b.x][b.y] != 0) {    // 如果子弹是遇到坦克们了
                            if (!b.mine && tanksScreenData[b.x][b.y] / 11 == 4) {
                                /*敌人的子弹和我的坦克*/
                                // TODO 游戏失败处理
                                // TODO 死亡动画
                                // TODO 过关 20分过关
                                // TODO 加BOSS
                                ABC = (short) (b.x + b.y * 10 + 3); // 记录失败坐标
                                t = -1;     // 做移除子弹的标记
                            } else if ((t = tanksScreenData[b.x][b.y] / 11) < 4 /*是敌人的坦克*/
                                    && tanks[t].visible) {                      /*这只坦克是活的*/
                                if (b.mine) {                   // 如果子弹是我的, 就让敌人死亡
                                    tanks[t].visible = false;   // 坦克消失/死亡
                                    control.Add(Control.SCORE); // 得分+1
                                    tankCount--;
                                    if (tankCount == 0) ABC = 2;     // 游戏胜利标记
                                    else ABC = 1;                    // 做需要刷新坦克图像的标记
                                }           // 否则子弹消失即可
                                t = -1;     // 做移除子弹的标记
                            } else if (tanksScreenData[b.x][b.y] == 65) {    // 是丛林
                                treeScreenData[b.x][b.y] = 55;               // 丛林消失
                                ABC = 1;                                     // 做需要刷新坦克的标记
                                t = -1;
                            }
                        } else if (bulletScreenData[b.x][b.y] != 0) {   // 如果遇到一个子弹[与上一时刻的对比]
                            t = bullets.size();
                            for (int j = 0; j < t; j++) {
                                if (i != j && bullets.get(j).x == b.x && bullets.get(j).y == b.y) {
                                    bullets.remove(j);
                                    t = -1;
                                    if (j < i) --i;
                                    break;
                                }
                            }
                        }
                        if (t == -1) {
                            bullets.remove(b);
                            --i;
                        }
                        for (int x = 0; x < 10; x++) Arrays.fill(bulletScreenData[x], 0); // 清空数据
                        for (Bullet c : bullets) bulletScreenData[c.x][c.y] = 5;          // 填上子弹
                    } // for - bullets
                    if (ABC == 1)
                        刷新坦克图像();
                    if (ABC == 2) {
                        // 清除屏幕上所有子弹遗迹
                        for (int x = 0; x < 10; x++) Arrays.fill(bulletScreenData[x], 0);
                        游戏胜利();
                    } else if (ABC >= 3) {
                        // 清除屏幕上所有子弹遗迹
                        for (int x = 0; x < 10; x++) Arrays.fill(bulletScreenData[x], 0);
                        ABC -= 3;
                        开始失败动画(ABC % 10, ABC / 10);
                    } else
                        刷新();
                } else if (1 == msg.what) {
                    /*坦克事件*/
                    // 出随机数, 做运动集合, 有上下左右, 不运动, 发子弹, 重复上次动作
                    int r;
                    //for (Tank t : tanks) {
                    Tank t = nextVisibleTank();
                    if (t != null) {        // 对于每一个活着的坦克
                        r = control.random.nextInt(13);
                        if (r >= 4 && r <= 6)
                            r = t.last;
                        else if (r != 8 && r != 10)     // 一共有3个发子弹的记录 但有两个不记录
                            t.last = r;
                        switch (r) {
                            case 0:
                                t.goLeft();
                                break;
                            case 1:
                                t.goRight();
                                break;
                            case 2:
                                t.goDown();
                                break;
                            case 3:
                                t.goUp();
                                break;
                            case 7:
                            case 8:
                            case 10:
                                发子弹(t);
                                break;
                            case 9:         // 静止不动
                                break;
                            case 11:
                                if (t.x < mTank.x) t.goRight();
                                else if (t.x > mTank.x) t.goLeft();
                                else 发子弹(t);
                                break;
                            case 12:
                                if (t.y < mTank.y) t.goDown();
                                else if (t.y > mTank.y) t.goUp();
                                else 发子弹(t);
                                break;
                        } // switch
                    } // if t. visible = true
                    // } // foreach tank
                    刷新坦克图像();
                    坦克出生();                 // 新出生的坦克没有运动权 (︶︹︺)
                    刷新坦克图像();
                    刷新();
                } // else if msg.what == 1
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
        bullets.add(new Bullet(t.getBulletX(), t.getBulletY(),
                t.dire, false));
    }

    private void 坦克出生() {
        for (Tank t : tanks) {
            if (!t.visible) {       // 寻找遇到一辆不可见的坦克
                if (control.random.nextInt(3) % 4 == 0) return;   // 降低出现新坦克的概率
                int x, y;
                boolean ok = true;
                // 寻找出生位置(最多试1次)
                // 生成1对坐标
                y = (int) (8.5f * control.random.nextInt(3));
                x = (7 * control.random.nextInt(2));
                for (int m = 0; m < 3 && ok; m++)
                    for (int n = 0; n < 3 && ok; n++)
                        ok = tanksScreenData[x + m][y + n] % 11 == 0;
                if (ok) {
                    t.x = x + 1;
                    t.y = y + 1;
                    t.visible = true;
                    break;          // 退出循环for
                }
                break;              // 退出循环foreach
            }                       // 否则测试下一辆坦克
        }
    }

    /**
     * 新关卡, 参数是是否让关卡+1, 第二个参数表示是否延迟
     */
    private void NewLevel(boolean NEW, boolean delay) {
        if (NEW) control.Add(Control.LEVEL);        // 关卡加一
        tankCount = 20;                             // 总共要出20个坦克
        bullets.clear();
        // 初始化我的坦克的位置, 以及把敌人坦克设置为隐藏
        for (int i = 0; i < 4; i++)
            tanks[i].visible = false;
        mTank.dire = 3;
        mTank.x = 4;
        mTank.y = 9;
        control.SetPreviewScreen(control.lifeImage[life]);
        // TODO 变更森林
        读取森林数据();
        // 启动计时器
        bulletTimer = new Timer();
        bulletTimer.schedule(new BulletTimerTask(), delay ? 300 : 0, 100);     // 每40毫秒子弹前进一格
        enemyTimer = new Timer();
        Lg.e("dalay = " + delay);
        enemyTimer.schedule(new EnemyTimerTask(), (delay ? 300 : 0), GetTime(speed));
        // 刷新图像

    }

    private long GetTime(int speed) {
        return 407L - 30 * speed;
    }

    private void 开始失败动画(int x, int y) {
        // TODO 播放声音
        Lg.e("失败于 x=" + x + " y=" + y);
        bulletTimer.cancel();
        bulletTimer = null;
        enemyTimer.cancel();
        enemyTimer = null;
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
        // TODO 播放声音
        Lg.e("游戏胜利");
        // 取消所有计时器
        bulletTimer.cancel();
        enemyTimer.cancel();
        bulletTimer = null;
        enemyTimer = null;
        control.startQuickShading();
        NewLevel(true, true);
    }

    private void 刷新() {
        for (int x = 0; x < 10; x++)
            for (int y = 0; y < 20; y++)
                screenData[x][y] = Math.max(tanksScreenData[x][y] % 11, bulletScreenData[x][y]);
        control.invalidate();
    }

    private int max_11(int a, int b) {
        if (a % 11 > b % 11) return a;
        return b;
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

        Tank(byte dire, int x, int y) {
            this.dire = dire;
            this.x = x;
            this.y = y;
            this.visible = false;
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
     * 自己的坦克
     * 主要是改一下输出, 因为自己的坦克多一块 （〜^㉨^)〜
     */
    private class MTank extends Tank {
        MTank(byte dire, int x, int y) {
            super(dire, x, y);
            visible = true;
        }

        @Override
        int getImage(int i) {
            switch (i) {
                case 0:
                    return dire == RIGHT || dire == DOWN ? 10 : 0;
                case 1:
                    return 10;
                case 2:
                    return dire == DOWN || dire == LEFT ? 10 : 0;
                case 3:
                    return 10;
                case 4:
                    return 10;
                case 5:
                    return 10;
                case 6:
                    return dire == RIGHT || dire == UP ? 10 : 0;
                case 7:
                    return 10;
                case 8:
                    return dire == LEFT || dire == UP ? 10 : 0;
                default:
                    return 0;
            }
        }
    }

    /**
     * 子弹
     */
    private class Bullet {
        int x, y;        // 坐标
        byte dire;       // 方向 0左 1上 2右 3下
        boolean mine;    // 是不是我发射的子弹

        /**
         * @param x    坐标X
         * @param y    坐标Y
         * @param dire 方向
         * @param mine 是不是我发的雷
         */
        Bullet(int x, int y, byte dire, boolean mine) {
            this.x = x;
            this.y = y;
            this.dire = dire;
            this.mine = mine;
        }

        /**
         * 返回运动是否成功
         * 如果运动失败或者撞墙是要取消的
         */
        boolean go() {
            if (dire == 1) return goLeft();
            else if (dire == 3) return goUp();
            else if (dire == 2) return goRight();
            else return goDown();
        }

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

    /**
     * 负责敌人运动和发射子弹的计时器
     */
    private class EnemyTimerTask extends TimerTask {
        @Override
        public void run() {
            handler.sendEmptyMessage(1);
        }
    }
}

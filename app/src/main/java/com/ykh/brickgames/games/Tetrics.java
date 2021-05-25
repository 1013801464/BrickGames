package com.ykh.brickgames.games;

import android.os.Handler;
import android.os.Message;
import android.util.SparseIntArray;

import com.ykh.brickgames.Lg;

import java.util.Arrays;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 俄罗斯方块
 */

class Tetrics implements Control.Game {
    private static int[][][][] BRICK;     // [方块类型][方向][X][Y]
    private static int[][] FULL_LINE_ANIMATION;
    private static int[] GET_SCORE = new int[5]; // 1行满+1分, 2行满+3分, 3行满+5分, 4行满+8分
    private int[][] hadBricks;            // 已存储的方块(都是24行的, 截取最后20行用来显示)
    private int[][] curBricks;            // 当前方块(都是24行的, 截取最后20行用来显示)
    private int curType;                  // 当前方块类型
    private int curRotate;                // 当前方块旋转方向
    private int nxtType;                  // 下一个方块类型
    private int nxtRotation;              // 下一个方块旋转方向
    private int cX, cY;                   // 中心方块坐标(x,y)
    private Control control;
    private Handler handler;
    private Timer timer;                  // Timer有多个用途, 可以随时重建
    private int Full_line;                // 满的一行的行号
    private int Full_line_count;          // 已经满过的行计数
    private int Full_timer_count;         // 动画过程计数
    private int[] Full_lines = new int[24];// 24个数, 指示这一行是否满了
    private boolean PreventAnyTouch = false;
    private int scoreNewLevel;            // 自升级所得分数累计
    private long speed;                   // 方块下移速度

    Tetrics(Control control) {
        this.control = control;
        hadBricks = new int[10][24];
        curBricks = new int[10][24];
        BRICK = new int[7][4][4][4];
        String b[] = new String[7];
        b[0] = "0100|1100|0100|0000,0000|1110|0100|0000," +
                "0100|0110|0100|0000,0100|1110|0000|0000";
        b[1] = "0000|0010|1110|0000,0000|0110|0010|0010," +
                "0000|0111|0100|0000,0100|0100|0110|0000";
        b[2] = "0110|1100|0000|0000,1000|1100|0100|0000," +
                "0110|1100|0000|0000,1000|1100|0100|0000";
        b[3] = "0000|1111|0000|0000,0100|0100|0100|0100," +
                "0000|1111|0000|0000,0100|0100|0100|0100";
        b[4] = "1100|1100|0000|0000,1100|1100|0000|0000," +
                "1100|1100|0000|0000,1100|1100|0000|0000";
        b[5] = "1100|0110|0000|0000,0100|1100|1000|0000," +
                "1100|0110|0000|0000,0100|1100|1000|0000";
        b[6] = "0010|0010|0110|0000,0000|1110|0010|0000," +
                "0000|0110|0100|0100,0000|0100|0111|0000";
        int count;
        for (int iType = 0; iType < 7; iType++) {
            count = 0;        // 每次切换方块类型都需要重新计数
            for (int iTime = 0; iTime < 4; iTime++)
                for (int iY = 0; iY < 4; iY++) {
                    for (int iX = 0; iX < 4; iX++)
                        BRICK[iType][iTime][iX][iY] = b[iType].charAt(count++) == '1' ? 10 : 0;
                    count++;    // 跳过竖线和逗号
                }
        }
        String c = "5555445555|5554334555|5543223455|" +        // 动画效果
                "5432112345|4321001234|3210000123|2100000012|" +
                "1000000001|0000000000";
        count = 0;
        FULL_LINE_ANIMATION = new int[9][10];
        for (int iTime = 0; iTime < 9; iTime++) {
            for (int iX = 0; iX < 10; iX++)
                FULL_LINE_ANIMATION[iTime][iX] = 2 * (c.charAt(count++) - '0');
            count++;
        }
        GET_SCORE[1] = 1;
        GET_SCORE[2] = 3;
        GET_SCORE[3] = 5;
        GET_SCORE[4] = 8;
    }

    // TODO 游戏失败处理
    private void 下落一格() {
        // 1. 刷新图像
        if (是否触地()) {       // 如果落地需要停止计数, 判断是否行满: 先试试
            timer.cancel();
            转换成已有方块();
            if ((Full_line = 获得已满行号()) != 24) {             // 如果行满, 清除这几行, 动画: 从下到上, 从中间散开
                PreventAnyTouch = true;
                Full_line_count = 0;                             // 表明已经销毁了0行, 加分用
                Arrays.fill(Full_lines, 0);
                timer = new Timer();                             // 新建行满动画
                timer.schedule(new LineFullRunnable(), 0, 30);   // 0.1秒右移1格
            } else {
                Lg.e("播放声音");
                control.playSound(2);
                生成新方块();
            }
        } else {        // 如果没有触地, 下落一格
            cY++;
            变更形状();
        }
    }

    /**
     * 生成随机的新方块, 然后开始计时
     */
    private boolean 生成新方块() {
        curType = nxtType;
        curRotate = nxtRotation;
        nxtType = control.random.nextInt(7);        // 随机方块类型
        nxtRotation = control.random.nextInt(4);    // 随机方块方向
        getOffset();                                // 修改偏移量(cx,cy)
        control.SetPreviewScreen(BRICK[nxtType][nxtRotation]);
        if (有内部冲突()) {
            PreventAnyTouch = true;
            onStop();
            control.endGame();
            return false;
        }
        timer = new Timer();                                // 生成完新方块新建时钟
        timer.schedule(new CountRunnable(), speed, speed);  // 延迟speed时间再开始
        return true;
    }

    private void 转换成已有方块() {
        for (int x = 0; x < 10; x++)
            for (int y = 0; y < 24; y++) {      // 从第4行开始, 转化成已有方块
                if (curBricks[x][y] != 0) {
                    hadBricks[x][y] = 10;
                    Lg.e("x=" + x + " y=" + y + "已经被销毁");
                    curBricks[x][y] = 0;
                }
            }
    }

    /**
     * 从最后一行开始, 逐行传递出结果
     */
    private int 获得已满行号() {
        boolean flag;                          // 这一行都有方块的情况下, F先置为真, 如果有空方块置为假
        for (int y = 23; y > 3; y--) {         // 同上, 从4行开始显示的
            flag = true;
            for (int x = 0; x < 10 && flag; x++) // 如果有一个方块为空, 循环退出
                if (hadBricks[x][y] == 0) flag = false;
            if (flag) return y;
        }
        return 24;                              // 如果为空说明没有行满, 返回第24行
    }

    private boolean 是否触地() {
        变更形状(false);
        for (int x = 0; x < 10; x++)
            if (curBricks[x][23] != 0) {         // 只要最后一行有东西, 下落一格一定触地
                Lg.e("因为 x=" + x + " y=" + 23 + "不空, 被判断为触地" + curBricks[x][23]);
                return true;
            }
        for (int x = 0; x < 10; x++)            // 如果某一行的一个方块存在东西, 并且下一格已存在东西
            for (int y = 22; y >= 0; y--)       // 则判断为触地
                if (curBricks[x][y] != 0 && hadBricks[x][y + 1] != 0) return true;
        return false;
    }

    @Override
    public void onStop() {
        Lg.e("俄罗斯方块 onStop执行");      // 好像没有正确执行终止算法
        if (timer != null) {
            Lg.e("并且销毁了计时器");
            timer.cancel();
            timer = null;                 // 销毁计时器
        }
    }

    /**
     * 只要发生了图像修改就要调用这里, 确保图像得以刷新
     */
    private void 变更形状() {
        变更形状(true);
    }

    private void 变更形状(boolean refresh) {
        for (int x = -1; x < 5; x++)
            for (int y = -1; y < 5; y++)
                if (x + cX >= 0 && x + cX < 10 && y + cY < 24 && y + cY >= 0) { // 必须在方块域内
                    if (x == -1 || x == 4 || y == -1 || y == 4)                 // 如果是边界直接归0
                        curBricks[x + cX][y + cY] = 0;
                    else
                        curBricks[x + cX][y + cY] = BRICK[curType][curRotate][x][y];
                }
        if (refresh) 刷新();
    }

    /**
     * 如果当前砖块和已有方块发生冲突则返回真, 否则返回假
     */
    private boolean 有内部冲突() {
        变更形状(false);
        for (int x = 0; x < 10; x++)
            for (int y = 0; y < 24; y++)      // 前4行也必须检查
                if (curBricks[x][y] != 0 && hadBricks[x][y] != 0)
                    return true;
        return false;
    }

    private boolean 有边界冲突() {
        if (cX < -1)
            return true;
        if (cX == -1 && (BRICK[curType][curRotate][0][0] != 0
                || BRICK[curType][curRotate][0][1] != 0
                || BRICK[curType][curRotate][0][2] != 0
                || BRICK[curType][curRotate][0][3] != 0)) return true;
        if (cX >= 7) {
            if (BRICK[curType][curRotate][3][0] != 0
                    || BRICK[curType][curRotate][3][1] != 0
                    || BRICK[curType][curRotate][3][2] != 0
                    || BRICK[curType][curRotate][3][3] != 0) return true;
            if (cX >= 8) {
                if (BRICK[curType][curRotate][2][0] != 0
                        || BRICK[curType][curRotate][2][1] != 0
                        || BRICK[curType][curRotate][2][2] != 0
                        || BRICK[curType][curRotate][2][3] != 0) return true;
                if (cX >= 9) {
                    if (BRICK[curType][curRotate][1][0] != 0
                            || BRICK[curType][curRotate][1][1] != 0
                            || BRICK[curType][curRotate][1][2] != 0
                            || BRICK[curType][curRotate][1][3] != 0) return true;
                }
            }
        }
        return false;
    }

    private void createHandler() {
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (0 == msg.what) 下落一格();
                else if (1 == msg.what) 行满动画();
                return true;
            }
        });
    }

    /**
     * 主要负责绘制动画, 也负责在结束的时候出新方块
     */
    private void 行满动画() {
        if (Full_timer_count == 0) {  // 还没有任何动画, 则获得行号
            Full_line = 获得已满行号();
            if (Full_line == 24) {    // 如果没有任何一行满了, 则
                timer.cancel();       // 关闭计时器, 退出函数
                timer = null;
                control.Add(Control.SCORE, GET_SCORE[Full_line_count]); // 加分
                scoreNewLevel += GET_SCORE[Full_line_count];
                if (scoreNewLevel >= 100) {
                    scoreNewLevel -= 100;
                    control.Add(Control.SPEED); // 速度+1
                    speed = GetSpeed(control.Get(Control.SPEED));
                }
                行满下移();
                PreventAnyTouch = false;        // TODO 依据prevent Any Touch区分是哪个timer
                if (!生成新方块()) return;       // 开始下一个方块(其中包含时钟!!) 上面参数必须在时钟之前!!
                变更形状(true);
                return;
            } else {
                Full_lines[Full_line] = 1; // 修改为打开
                Full_line_count++;
                control.playSound(1);
            }
        }
        for (int x = 0; x < 10; x++)  // 显示动画: 按动画把这一行复制过去
            hadBricks[x][Full_line] = FULL_LINE_ANIMATION[Full_timer_count][x];
        刷新();
        // 动画一共9帧, 0-8, 所以如果到达9要重新开始
        // 计时器不需要取消, 新一行有新一行的count
        Full_timer_count = (Full_timer_count == 8 ? 0 : Full_timer_count + 1);
    }

    // 因行满使所有方块下移

    private void 行满下移() {
        int count = 23;
        int y = 23;
        while (count >= 0) {
            while (Full_lines[count] == 1 && count >= 0) count--;
            Full_lines[y--] = count--;
        }
        while (y >= 0) Full_lines[y--] = 0;
        for (y = 23; y >= 0; --y) {           // y是复制到, FullLines[y]是从哪复制
            if (0 == Full_lines[y])
                for (int x = 0; x < 10; x++) hadBricks[x][y] = 0;
            else
                for (int x = 0; x < 10; x++) hadBricks[x][y] = hadBricks[x][Full_lines[y]];
        }
    }

    private long GetSpeed(int speed) {
        return 1100 - 100 * speed;
    }

    @Override
    public void onLeft() {
        if (!PreventAnyTouch) {
            cX--;                                   // 先让中心坐标减了
            if (有边界冲突() || 有内部冲突()) cX++;   // 如果左移以后发生了冲突, 需要恢复

            else 变更形状();

        }
    }

    @Override
    public void onRight() {
        if (!PreventAnyTouch) {
            cX++;
            if (有边界冲突() || 有内部冲突()) cX--;       // 如果右移发生冲突, 撤销修改
            else 变更形状();
        }
    }


    @Override
    public void onUp() {
        if (!PreventAnyTouch) {
            curType = (curType + 1) % 7;
            变更形状();
        }
    }

    @Override
    public void onDown() {
        if (!PreventAnyTouch) {
        /*curType = (curType + 6) % 7;
        变更形状();*/
            下落一格();
        }
    }

    @Override
    public void onRotate() {
        if (!PreventAnyTouch) {
            curRotate = (curRotate + 1) % 4;
            if (有内部冲突() || 有边界冲突())          // 如果旋转发生冲突需要恢复
                curRotate = (curRotate + 3) % 4;
            else                    // 没有冲突说明旋转成功了, 刷新一下 ๑乛◡乛๑
                变更形状();
        }
    }

    @Override
    public void onStart(SparseIntArray Option) {
        Lg.e("俄罗斯方块游戏 onStart()");
        speed = GetSpeed(Option.get(2));
        int level = 24 - Option.get(3);          // level是预加行数
        for (int y = 23; y > level; y--) {
            for (int x = 0; x < 10; x++) {
                hadBricks[x][y] = control.random.nextBoolean() ? 10 : 0;
            }
        }
        for (int i = 0; i < 10; i++) {
            Arrays.fill(hadBricks[i], 0);
            Arrays.fill(curBricks[i], 0);
        }
        createHandler();
        nxtType = control.random.nextInt(7);        // 随机方块类型
        nxtRotation = control.random.nextInt(4);    // 随机方块方向
        生成新方块();
        变更形状();
        PreventAnyTouch = false;
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
        timer = new Timer();        // 恢复里面新建时钟
        Lg.e("俄罗斯方块游戏恢复");
        if (PreventAnyTouch) {
            // timer 为lineFullRunnable
            timer.schedule(new LineFullRunnable(), 0, 30);   // 0.1秒右移1格
        } else {
            // timer为下落的计时器: 普通
            timer.schedule(new CountRunnable(), speed / 2, speed);
        }
    }

    // TODO 方法在这里, 位置以后矫正
    private void getOffset() {
        cX = 3;
        cY = 1;
        switch (curType) {
            case 0:
                if (curRotate == 3) cY = 1;
                else cY = 2;
                break;
            case 1:
                break;
            case 2:
                break;
            case 3: // 横线 1,3是横的
                if (curRotate == 1 || curRotate == 3)
                    cY = 3;
                break;
            case 4:
                cX = 4;
                cY = 2;
                break;
            case 5:
                break;
            case 6:
                break;
        }
    }

    // TODO 改变这里的加4
    private void 刷新() {
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 20; y++) {
                control.screenData[x][y] = Math.max(hadBricks[x][y + 4], curBricks[x][y + 4]);
            }
        }
        control.invalidate();
    }

    @Override
    public int getScreenData(int x, int y) {
        return control.screenData[x][y];
    }

    /**
     * 负责计数的Runnable
     * 在普通状态下计数
     * 在落地时, 如果行满则停止计数
     * 按暂停时, 停止计数, 并记录已有时间
     */
    private class CountRunnable extends TimerTask {
        @Override
        public void run() {
            if (handler != null) {
                handler.sendEmptyMessage(0);
            }
        }
    }

    /**
     * 行满执行部分
     */
    private class LineFullRunnable extends TimerTask {
        @Override
        public void run() {
            handler.sendEmptyMessage(1);
        }
    }

}

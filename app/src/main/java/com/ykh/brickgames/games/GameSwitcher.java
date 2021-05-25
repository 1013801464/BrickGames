package com.ykh.brickgames.games;

import android.os.Handler;
import android.os.Message;
import android.util.SparseIntArray;

import com.ykh.brickgames.Lg;

import java.util.Arrays;
import java.util.Timer;

/**
 * 本类用来切换游戏
 */

class GameSwitcher implements Control.Game {
    private static int maxGame = 8;     // 7是网络游戏
    private static int[] maxOption;     // 每个游戏最大能设置的选项个数
    private Control control;
    private Handler handler;
    private int mOption;                // 当前选项
    private int mGame;                  // 当前选择的游戏(屏幕上显示的数字-1)
    private int[][][] numdata;
    private int[][][][] numDescription; // 描述数据data[关卡][帧][图像X][图像Y]
    private Timer timer;
    private int currentPage = 0;        // 正在显示第几帧

    GameSwitcher(Control control) {
        this.control = control;
        maxOption = new int[maxGame];
        maxOption[0] = 48;
        maxOption[1] = 4;
        maxOption[2] = 4;
        maxOption[3] = 4;
        maxOption[4] = 4;
        maxOption[5] = 4;
        maxOption[6] = 1;
        maxOption[7] = 1;
    }

    private void changeGame() {
        int g = (mGame + 1) % 10;
        int sh = (mGame + 1) / 10 % 10;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                control.screenData[2 + i][j] = numdata[sh][i][j];
                control.screenData[6 + i][j] = numdata[g][i][j];
            }
        }
        currentPage = 3;
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new TimerTask(), 0, 500);    // 即刻开始, 周期1秒
    }

    private void changeOption() {
        int og = (mOption + 1) % 10;
        int osh = (mOption + 1) / 10 % 10;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                control.screenData[2 + i][15 + j] = numdata[osh][i][j];
                control.screenData[6 + i][15 + j] = numdata[og][i][j];
            }
        }
        control.invalidate();
    }

    @Override
    public void onStart(SparseIntArray Option) {
        Lg.e("游戏切换器 Onstart()");
        numdata = new int[10][3][5];
        初始化数字();
        初始化动图();
        初始化Handler();
        for (int x = 0; x < 10; x++)
            Arrays.fill(control.screenData[x], 0);  // 显示屏填充为0
        changeGame();
        changeOption();
    }

    private void 初始化动图() {
        if(numDescription != null) return;
        int count;
        String g[] = new String[maxGame];
        g[0] =  "0001000000" +      /*First Frame*/
                "0011000000" +
                "0001000000" +
                "0000000000" +
                "0000000000" +
                "1000000000" +
                "1100010111" +
                "1110110111|" +
                "0000000000" +      /*Second Frame*/
                "0000000000" +
                "0000000000" +
                "0000000000" +
                "0000000000" +
                "1001000000" +
                "1111010111" +
                "1111110111|" +
                "0000011000" +
                "0000001000" +
                "0000001000" +
                "0000000000" +
                "0000000000" +
                "1001000000" +
                "1111010111" +
                "1111110111|" +
                "0000000000" +
                "0000000000" +
                "0000000000" +
                "0000000000" +
                "0000000000" +
                "0000000000" +
                "1001011000" +
                "1111011111";
        g[1] =  "0001010000" +  // 1.1
                "0001110000" +  // 1.2
                "0000100000" +  // 1.3
                "0000010000" +  // 1.4
                "0000000000" +  // 1.5
                "0000010000" +  // 1.6
                "0000111000" +  // 1.7
                "0000111000|" +  // 1.8
                "0001010000" +  // 2.1
                "0001110000" +  // 2.2
                "0000110000" +  // 2.3
                "0000000000" +  // 2.4
                "0000000000" +  // 2.5
                "0000010000" +  // 2.6
                "0000111000" +  // 2.7
                "0000111000|" +  // 2.8
                "0000000000" +
                "0000000000" +
                "0000000000" +
                "0000000000" +
                "0000000000" +
                "0000010000" +
                "0000111000" +
                "0000111000|" +
                "1100000000" +
                "0110000000" +
                "1100000000" +
                "0000000000" +
                "0000010000" +
                "0000111000" +
                "0000111000" +
                "0000000000";
        g[2] =  "1000001001" +  // Frame1
                "1000011101" +  // 2
                "1000001001" +  // 3
                "0000010100" +  // 4
                "1001000001" +  //
                "1011100001" +  //
                "1001000001" +  //
                "0010100000|" + //
                "1000000001" +  // Frame 2
                "1001001001" +  //2
                "1011111101" +
                "0001001000" +  // 4
                "1010110101" +
                "1000000001" +
                "1000000001" +
                "0000000000|" +
                "1001000001" +  // Frame 3
                "0011100000" +
                "1001000001" +
                "1010100001" + // 4
                "1000001001" +  // 5
                "0000011100" +  // 6
                "1000001001" +  // 7
                "1000010101|" +
                "1001000001" +  // Frame 4
                "1010100001" +
                "0000000000" + // 3
                "1000000001" +
                "1000000001" +  // 5
                "1000000001" +
                "0000001000" +
                "1000011101";
        g[3] =  "1111111111" +  // 1.1
                "0111100110" +  // 1.2
                "0100000000" +  // 1.3
                "0000000000" +  // 1.4
                "0000000000" +  // 1.5
                "0000100000" +  // 1.6
                "0000100000" +  // 1.7
                "0001110000|" + // 1.8
                "1111111111" +  // 2.1
                "0111100110" +  // 2.2
                "0100000000" +  // 2.3
                "0000100000" +  // 2.4
                "0000000000" +  // 2.5
                "0000000000" +  // 2.6
                "0000100000" +  // 2.7
                "0001110000|" + // 2.8
                "1111111111" +  // 3.1
                "0111100110" +  // 3.2
                "0100100000" +  // 2.3
                "0000000000" +  // 2.4
                "0000000000" +  // 2.5
                "0000000000" +  // 2.6
                "0000100000" +  // 2.7
                "0001110000|" + // 2.8
                "1111111111" +  // 4.1
                "0111000110" +  // 4.2
                "0100000000" +  // 4.3
                "0000000000" +  // 4.4
                "0000000000" +  // 4.5
                "0000000000" +  // 4.6
                "0000100000" +  // 4.7
                "0001110000";   // 4.8
        g[4] =  "1101011111" +
                "1111110011" +
                "1110111111" +
                "0000000000" +
                "0000000000" +
                "0000000000" +
                "0000010000" +
                "0000111000|" +
                "1101011111" +  // 2.1
                "1111110011" +
                "1110111111" +
                "0000000000" +
                "0001000000" +
                "0000000000" +
                "0001000000" +
                "0011100000|" +
                "1101011111" +  // 3.1
                "1111110011" +
                "1111111111" +
                "0000000000" +
                "0000000000" +
                "0000000000" +
                "0001000000" +
                "0011100000|" +
                "1101011111" +  // 4.1
                "1111110011" +
                "0000000000" +
                "0000000000" +
                "0000000000" +
                "0000000000" +
                "0001000000" +
                "0011100000";
        g[5] =  "1111111111" +  // 1.1
                "0111111110" +  // 1.2
                "0000000000" +  // 1.3
                "0000000000" +  // 1.4
                "0000000000" +  // 1.5
                "0000000000" +  // 1.6
                "0000010000" +  // 1.7
                "0000011110|" + // 1.8
                "1111111111" +  // 2.1
                "0111111110" +  // 2.2
                "0000000000" +  // 2.3
                "0000000000" +  // 2.4
                "0001000000" +  // 2.5
                "0000000000" +
                "0000000000" +
                "0000111100|" + // 2.8
                "1111111111" +  // 3.1
                "0111111110" +  // 3.2
                "0100000000" +  // 3.3
                "0000000000" +  // 3.4
                "0000000000" +  // 3.5
                "0000000000" +
                "0000000000" +
                "0001111000|" + // 3.8
                "1111111111" +  // 3.1
                "0011111110" +  // 3.2
                "0000000000" +  // 3.3
                "0000000000" +  // 3.4
                "0000000000" +  // 3.5
                "0000000000" +
                "0000000000" +
                "0001111000";
        g[6] = "0000000000" +
                "0011001000" +
                "0100101000" +
                "0100101000" +
                "0011001110" +
                "0011001110" +
                "0011001110" +
                "0000000000|" +
                "0000000000" +
                "1010101010" +
                "0101010101" +
                "0101010101" +
                "0101010101" +
                "0101010101" +
                "1010101010" +
                "1010101010|" +
                "0000000000" +
                "0101010101" +
                "0101010101" +
                "0101010101" +
                "0101010101" +
                "1010101010" +
                "1010101010" +
                "0101010101|" +
                "0000000000" +
                "1010101010" +
                "0101010101" +
                "0101010101" +
                "0101010101" +
                "1010101010" +
                "0101010101" +
                "1010101010";
        g[7] =  "0001010000" +  // 1.1
                "0001110000" +  // 1.2
                "0000100000" +  // 1.3
                "0000010000" +  // 1.4
                "0000000000" +  // 1.5
                "0000010000" +  // 1.6
                "0000111000" +  // 1.7
                "0000101000|" +  // 1.8
                "0001010000" +  // 2.1
                "0001110000" +  // 2.2
                "0000110000" +  // 2.3
                "0000000000" +  // 2.4
                "0000000000" +  // 2.5
                "0000010000" +  // 2.6
                "0000111000" +  // 2.7
                "0000101000|" +  // 2.8
                "0000000000" +
                "0000000000" +
                "0000000000" +
                "0000000000" +
                "0000000000" +
                "0000010000" +
                "0000111000" +
                "0000101000|" +
                "1100000000" +
                "0110000000" +
                "1100000000" +
                "0000000000" +
                "0000010000" +
                "0000111000" +
                "0000101000" +
                "0000000000";
        numDescription = new int[maxGame][4][10][8];
        for (int iGame = 0; iGame < maxGame; iGame++) {
            count = 0;
            for (int iTime = 0; iTime < 4; iTime++) {
                for (int iY = 0; iY < 8; iY++) {
                    for (int iX = 0; iX < 10; iX++) {
                        numDescription[iGame][iTime][iX][iY] = g[iGame].charAt(count++) == '1' ? 10 : 0;
                    }
                }
                count++;
            }
        }
    }

    private void 初始化数字() {
        int count = 0;
        String b = "000010010010000|101001101101000|000110000011000|" +
                "000110000110000|010010000110110|000011000110000|" +
                "000011000010000|000110110110110|000010000010000|" +
                "000010000110000";
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 5; j++)
                for (int k = 0; k < 3; k++)
                    numdata[i][k][j] = b.charAt(count++) == '1' ? 0 : 10;
            count++;
        }
    }

    private void 初始化Handler() {
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                // 刷新描述图(中间的动图)
                currentPage = (currentPage + 1) % 4;
                for (int iX = 0; iX < 10; iX++) {
                    System.arraycopy(numDescription[mGame][currentPage][iX], 0, control.screenData[iX], 6, 8);
                }
                control.invalidate();
                return true;
            }
        });
    }

    @Override
    public void onLeft() {
        control.Add(Control.LEVEL);
    }

    @Override
    public void onRight() {
        control.Add(Control.SPEED);
    }

    @Override
    public void onUp() {
        mOption = (mOption + 47) % maxOption[mGame];
        changeOption();
    }

    @Override
    public void onDown() {
        mOption = (mOption + 1) % maxOption[mGame];
        changeOption();
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
        timer = new Timer();
        timer.schedule(new TimerTask(), 500, 500);    // 周期1秒
    }

    @Override
    public void onRotate() {
        mGame = (mGame + 1) % maxGame;
        if (mOption >= maxOption[mGame]) {
            mOption = maxOption[mGame] - 1;
            changeOption();
        }
        changeGame();
    }

    short[] getResult() {
        short[] t = new short[2];
        t[0] = (short) mGame;
        t[1] = (short) mOption;
        return t;
    }

    /**
     * TODO 销毁所有数据
     * 注意: 不许调用control.endGame();
     */
    public void onStop() {
        Lg.e("执行游戏选择器的onStop");
        if (timer != null) {
            Lg.e("timer已取消");
            timer.cancel();
            timer = null;
        }
        handler = null;
    }

    @Override
    public int getScreenData(int x, int y) {
        return control.screenData[x][y];
    }

    private class TimerTask extends java.util.TimerTask {
        @Override
        public void run() {
            handler.sendEmptyMessage(0);
        }
    }

}

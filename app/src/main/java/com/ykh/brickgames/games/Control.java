package com.ykh.brickgames.games;

import android.os.Handler;
import android.os.Message;
import android.util.SparseIntArray;

import com.ykh.brickgames.Lg;
import com.ykh.brickgames.MainActivity;
import com.ykh.brickgames.myViews.ControllerScreen;
import com.ykh.brickgames.myViews.GameScreen;
import com.ykh.brickgames.myViews.ScoreScreen;

import java.util.Arrays;
import java.util.Random;
import java.util.Timer;

/**
 * 核心控制器
 */

public class Control implements ControllerScreen.Call {
    static final byte SCORE = 0x1;
    static final byte SPEED = 0x2;
    static final byte LEVEL = 0x3;
    public static int HI_SCORE = 0;    // 最高分
    int[][][] lifeImage;                // [几条命的图像][x轴][y轴], 注 lifeImage[4]是空图
    int[][] screenData;                 // 共享屏幕数据 节省内存
    Random random = new Random();
    private boolean paused = false;
    private boolean pausedFromButton = true;
    private boolean preventTouch = false;
    private Game[] games;
    private short mGame = 0;
    private int[] shade;
    private GameScreen gameView;
    private ScoreScreen scoreView;
    private MainActivity context;
    private NormalScreenAdapter normalScreenAdapter;
    private AnimationScreenAdapter animationScreenAdapter;
    private Timer timerA;               // timerA是游戏结束动画
    private Timer timerB;               // timerB是快动画时用的
    private short shadeCount;
    private boolean overturn = false;
    private Handler handler;

    public Control(GameScreen main, ScoreScreen scoreView, MainActivity context) {
        screenData = new int[10][20];
        lifeImage = new int[5][4][4];
        normalScreenAdapter = new NormalScreenAdapter();
        animationScreenAdapter = new AnimationScreenAdapter();
        gameView = main;
        gameView.setData(normalScreenAdapter);
        for (int iLife = 0; iLife <= 4; iLife++) {
            Arrays.fill(lifeImage[iLife][0], 0);
            Arrays.fill(lifeImage[iLife][3], 0);
            for (int iY = 0; iY < 4; iY++) {
                lifeImage[iLife][1][iY] = iLife >= (4 - iY) ? 1 : 0;
                lifeImage[iLife][2][iY] = iLife >= (4 - iY) ? 1 : 0;
            }
        }
        games = new Game[10];
        games[0] = new Start(this);
        games[1] = new GameSwitcher(this);
        games[2] = new Tetrics(this);
        games[3] = new TankGame(this);
        games[4] = new CarRacing(this);
        games[5] = new MoveMinus(this);
        games[6] = new MoveAdd(this);
        games[7] = new SingleBounce(this);
        games[8] = new NetGame(this);
        games[9] = new NetTankGame(this);
        this.scoreView = scoreView;
        this.context = context;
        Set(SCORE, HI_SCORE);
        games[0].onStart(null);
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == 1) {
                    mGame = 1;
                    games[1].onStart(null);
                    invalidate();
                    preventTouch = false;
                } else
                    gameView.invalidate();
                return true;
            }
        });
    }

    private void Set(byte field, int value) {
        scoreView.set(field, value);
        if (field == SCORE && value > HI_SCORE) {
            HI_SCORE = value;
            scoreView.setHighScoreEnabled(true);
        }
    }

    int Get(byte field) {
        return scoreView.get(field);
    }

    void Add(byte field) {
        Add(field, 1);
    }

    void Add(byte field, int value) {
        scoreView.add(field, value);
        if (field == SCORE && scoreView.get(SCORE) > HI_SCORE) {
            HI_SCORE = scoreView.get(SCORE);
            scoreView.setHighScoreEnabled(true);
        }
    }

    private void ViewReset() {
        scoreView.setGameOverEnabled(true); // 显示GameOver
        scoreView.setPreviewScreen(lifeImage[0]);
        scoreView.setHighScoreEnabled(true);
        scoreView.set(SCORE, HI_SCORE);
    }

    /**
     * 设置预览区图像
     */
    void SetPreviewScreen(int[][] v) {
        scoreView.setPreviewScreen(v);
    }

    /**
     * 复位键单击
     */
    public void onClickReset() {
        if (timerB != null) {
            timerB.cancel();
            timerB = null;
            gameView.setData(normalScreenAdapter);
        }
        if (timerA != null) {
            timerA.cancel();
            timerA = null;          // 做标记: 不用再执行了
            gameView.setData(normalScreenAdapter);
        } else {                    // 如果计时器本来就没有运行说明是正常状态
            Lg.e("reset单击因此onstop() mGame=" + mGame);
            games[mGame].onStop();
            ViewReset();
        }
        mGame = 0;
        games[0].onStart(null);     //
        paused = false;
    }

    @Override
    public void onClickLeft() {
        if (mGame >= 2 && paused) return;
        games[mGame].onLeft();
    }

    @Override
    public void onClickRight() {
        if (mGame >= 2 && paused) return;
        games[mGame].onRight();
    }

    @Override
    public void onClickUp() {
        if (mGame >= 2 && paused) return;
        games[mGame].onUp();
    }

    @Override
    public void onClickDown() {
        if (mGame >= 2 && paused) return;
        games[mGame].onDown();
    }

    public void onClickRotate() {
        if (mGame >= 2 && paused) return;
        games[mGame].onRotate();
    }


    // 大暂停, 大回复由系统调用
    public void onBigPause() {
        Lg.e("大暂停!!");
        if (!paused) {
            pausedFromButton = false;
            paused = true;
            Lg.e("preventTouch=" + preventTouch);
            if (!preventTouch)
                games[mGame].onPause();
            if (timerA != null) {
                timerA.cancel();
            }
        }
    }

    public void onBigResume() {
        Lg.e("大恢复!!");
        Lg.e("pausedFromButton = " + pausedFromButton + "  pause = " + paused);
        if (!pausedFromButton && paused) {
            paused = false;
            if (!preventTouch) {
                Lg.e("preventtouch为false故执行恢复");
                games[mGame].onResume();
            }
            if (timerA != null) {
                timerA = new Timer();
                timerA.schedule(new TimerTask(), 25, 50);
            }
        }
    }

    public void onDestroy() {
        Lg.e("结束程序");
        if (timerA != null) {
            timerA.cancel();
            timerA = null;
        }
        if (timerB != null) {
            timerB.cancel();
            timerB = null;
        }
        games[mGame].onStop();
    }

    public void onClickPause() {
        Lg.e("onClickPause() called mGame=" + mGame);
        if (mGame == 0) return;
        if (mGame == 1) {
            games[1].onStop();
            endGame();
            return;
        }
        if (timerA == null) {        // 计时器执行期间禁止执行暂停
            if (paused) {
                paused = false;
                pausedFromButton = false;
                scoreView.setPaused(false);
                games[mGame].onResume();
            } else {
                paused = true;
                pausedFromButton = true;
                scoreView.setPaused(true);
                games[mGame].onPause();
            }
        }
    }

    /**
     * 负责游戏切换的方法
     * 如果当前是0[开始动画] 则切换到1[游戏选择器]
     * 如果是1, 则打开相应的游戏
     * 如果是其它游戏切换到游戏1
     */
    void endGame() {
        if (mGame == 0) {
            mGame = 1;
            games[1].onStart(null);
            invalidate();
        } else if (mGame == 1) {
            short[] t = ((GameSwitcher) games[1]).getResult();
            mGame = (short) (t[0] + 2);
            SparseIntArray option = new SparseIntArray();
            option.put(1, t[1]);           // 1: Option, 即下面显示的数字, 取值 0-3
            option.put(2, Get(SPEED));     // 2: speed : 取值 1-10
            option.put(3, Get(LEVEL));     // 3: level : 取值 1-10
            option.put(4, 0);              // 4: 俄罗斯方块高级选项!
            Set(SCORE, 0);
            scoreView.setPaused(false);
            scoreView.setHighScoreEnabled(false);
            scoreView.setGameOverEnabled(false);
            games[mGame].onStart(option);
            invalidate();
            Lg.e("刷新图像");
        } else {
            preventTouch = true;
            ViewReset();
            startShading();
        }
    }

    void invalidate() {
        gameView.invalidate();
    }

    void playSound(int sound) {
        context.playSound(sound);
    }

    void startShading() {
        shade = new int[20];
        shadeCount = 1;
        gameView.setData(animationScreenAdapter);
        timerA = new Timer();
        timerA.schedule(new TimerTask(), 0, 50);
    }

    void startQuickShading() {
        shade = new int[20];
        shadeCount = 1;
        overturn = false;
        gameView.setData(animationScreenAdapter);
        timerB = new Timer();
        timerB.schedule(new TimerTask() {
            @Override
            public void run() {
                if (overturn) {
                    for (int i = 19; i >= shadeCount && i >= 0; i--)
                        if (shade[i] > 0) shade[i]--;
                    shadeCount--;
                    if (shadeCount == -11) {
                        timerB.cancel();
                        gameView.setData(normalScreenAdapter);
                    }
                    handler.sendEmptyMessage(0);
                } else {
                    for (int i = 0; i < shadeCount && i < 20; i++)
                        if (shade[i] < 10) shade[i]++;
                    shadeCount++;
                    if (shadeCount == 31) {
                        overturn = true;
                        shadeCount = 19;
                    }
                    handler.sendEmptyMessage(0);
                }
            }
        }, 0, 10);
    }

    interface Game {
        void onLeft();

        void onRight();

        void onUp();

        void onDown();

        void onRotate();

        /*游戏开始时触发*/
        void onStart(SparseIntArray Option);

        void onPause();

        void onResume();

        void onStop();

        int getScreenData(int x, int y);        // 被动获取图像
    }

    private class NormalScreenAdapter implements GameScreen.Adapter {
        @Override
        public int getScreenData(int x, int y) {
            return games[mGame].getScreenData(x, y);
        }
    }

    private class AnimationScreenAdapter implements GameScreen.Adapter {
        @Override
        public int getScreenData(int x, int y) {
            return Math.max(games[mGame].getScreenData(x, y), shade[y]);
        }
    }

    private class TimerTask extends java.util.TimerTask {
        @Override
        public void run() {
            for (int i = 0; i < shadeCount && i < 20; i++)
                if (shade[i] < 10) shade[i]++;
            shadeCount++;
            if (shadeCount == 31) {
                timerA.cancel();
                timerA = null;
                gameView.setData(normalScreenAdapter);
                handler.sendEmptyMessage(1);
            } else
                handler.sendEmptyMessage(0);
        }
    }
}

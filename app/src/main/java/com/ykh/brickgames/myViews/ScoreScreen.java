package com.ykh.brickgames.myViews;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.ykh.brickgames.R;


/**
 * 游戏机右侧现实的东西
 * TODO 定位每个图片的位置, 在onSize时处理
 */

public class ScoreScreen extends View {
    public static final byte SCORE = 1;
    public static final byte SPEED = 2;
    public static final byte LEVEL = 3;
    private static String TAG = "ScoreScreen";
    private float width;            // 屏幕宽度
    private float height;           // 屏幕高度
    private Paint paint;            // 画笔
    private Paint paintClear;       // 用来清空某一块的画笔
    private Bitmap[] bmpNumbers;    // 液晶数字
    private Bitmap bmpHi_Score;     // 最高分标签
    private Bitmap bmpMusic;        // 音乐标签
    private Bitmap bmpSpeedLevel;   // 速度等级标签
    private Bitmap bmpPause;        // 暂停
    private Bitmap bmpGameOver;     // 游戏结束
    private Bitmap bmpBrick;        // 砖块图片
    private Rect sourceRect_Num;    // 图像切割源
    private RectF destRect_Num;     // 目标位置
    //最高分标签
    private Rect sourceRect_Hi_Score;
    private RectF destRect_Hi_Score;
    private Rect sourceRect_Music;
    private RectF destRect_Music;
    private Rect sourceRect_GameOver;
    private RectF destRect_GameOver;
    private Rect sourceRect_Pause;
    private RectF destRect_Pause;
    private Rect sourceRect_SpeedLevel;
    private RectF destRect_SpeedLevel;
    private RectF[] destRect_SpeedNum;
    private RectF[] destRect_LevelNum;
    private RectF[][] destRect_16Bricks;
    private Rect sourceRect_16Bricks;
    private float topNumbersY;      // 顶层数字Y坐标  X是水平方向, Y是垂直方向
    private float[] topNumbersX;    // 顶层数字X坐标
    private float numberWidth;
    private float numberHeight;
    private int score;              // score默认为0
    private int speed = 1;          // speed默认为0
    private int level = 1;          // level默认为0
    private int[][] preview;        // 预览区数据
    private boolean isHighScore = true;    // 是否最高分
    private boolean paused = true;         // 已暂停
    private boolean soundEnabled = true;   // 声音是否打开
    private boolean gameOverEnabled = true;// 游戏结束标记

    public ScoreScreen(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ScoreScreen(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScoreScreen(Context context) {
        super(context);
        init();
    }


    public void set(byte Field, int number) {
        switch (Field) {
            case SCORE:
                if (score > 999999)
                    score = 999999;
                else
                    score = number;
                break;
            case LEVEL:
                if (level > 10)
                    level = 1;
                else
                    level = number;
                break;
            case SPEED:
                if (speed > 10)
                    speed = 1;
                else
                    speed = number;
                break;
        }
        invalidate((int) topNumbersX[0], (int) topNumbersY, (int) (topNumbersX[5] + numberWidth),
                (int) (topNumbersY + numberHeight));
    }

    public void add(byte Field, int number) {
        switch (Field) {
            case SCORE:
                score += number;
                invalidate((int) topNumbersX[0], (int) topNumbersY, (int) (topNumbersX[5] +
                        numberWidth), (int) (topNumbersY + numberHeight));
                break;
            case LEVEL:
                level += number;
                if (level > 10) level = 1;
                invalidate();   // TODO 让这里精确化
                break;
            case SPEED:
                speed += number;
                if (speed > 10) speed = 1;
                invalidate();
                break;
        }
    }

    /**
     * 参数是四行四列的数据, 显示在预览区
     */
    public void setPreviewScreen(int[][] data) {
        if (null == data || data.length < 4) return;
        for (int i = 0; i < 4; i++)
            if (data[i].length < 4) return;
        preview = data;
        invalidate((int) destRect_16Bricks[0][0].left, (int) destRect_16Bricks[0][0].top,
                (int) destRect_16Bricks[3][3].right, (int) destRect_16Bricks[3][3].bottom);
    }

    public int get(int Field) {
        switch (Field) {
            case SCORE:
                return score;
            case SPEED:
                return speed;
            case LEVEL:
                return level;
        }
        return 0;
    }

    public void setHighScoreEnabled(boolean value) {
        if (value != isHighScore) {
            isHighScore = value;
            invalidate((int) destRect_Hi_Score.left, (int) destRect_Hi_Score.top,
                    (int) destRect_Hi_Score.right, (int) destRect_Hi_Score.bottom);
        }
    }

    public void setPaused(boolean value) {
        if (value != paused) {
            this.paused = value;
            invalidate();
        }
    }

    public void setSoundEnabled(boolean value) {
        if (soundEnabled != value) {
            soundEnabled = value;
            invalidate((int) destRect_Music.left, (int) destRect_Music.top,
                    (int) destRect_Music.right, (int) destRect_Music.bottom);
        }
    }

    /**
     * 设置游戏结束标记
     */
    public void setGameOverEnabled(boolean value) {
        if (gameOverEnabled != value) {
            gameOverEnabled = value;
            invalidate((int) destRect_GameOver.left, (int) destRect_GameOver.top,
                    (int) destRect_GameOver.right, (int) destRect_GameOver.bottom);
        }
    }

    private void init() {
        // 初始化画笔
        paint = new Paint();
        paint.setDither(true);
        paint.setAntiAlias(true);
        paintClear = new Paint();
        paintClear.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        // 获得9个液晶数字的图像
        bmpNumbers = new Bitmap[10];
        bmpNumbers[0] = BitmapFactory.decodeResource(getResources(), R.drawable.r0);
        bmpNumbers[1] = BitmapFactory.decodeResource(getResources(), R.drawable.r1);
        bmpNumbers[2] = BitmapFactory.decodeResource(getResources(), R.drawable.r2);
        bmpNumbers[3] = BitmapFactory.decodeResource(getResources(), R.drawable.r3);
        bmpNumbers[4] = BitmapFactory.decodeResource(getResources(), R.drawable.r4);
        bmpNumbers[5] = BitmapFactory.decodeResource(getResources(), R.drawable.r5);
        bmpNumbers[6] = BitmapFactory.decodeResource(getResources(), R.drawable.r6);
        bmpNumbers[7] = BitmapFactory.decodeResource(getResources(), R.drawable.r7);
        bmpNumbers[8] = BitmapFactory.decodeResource(getResources(), R.drawable.r8);
        bmpNumbers[9] = BitmapFactory.decodeResource(getResources(), R.drawable.r9);
        bmpHi_Score = BitmapFactory.decodeResource(getResources(), R.drawable.hi_score);
        bmpMusic = BitmapFactory.decodeResource(getResources(), R.drawable.music);
        bmpSpeedLevel = BitmapFactory.decodeResource(getResources(), R.drawable.speed_level);
        bmpPause = BitmapFactory.decodeResource(getResources(), R.drawable.pause);
        bmpGameOver = BitmapFactory.decodeResource(getResources(), R.drawable.game_over);
        bmpBrick = BitmapFactory.decodeResource(getResources(), R.drawable.brick);

        destRect_SpeedNum = new RectF[2];
        destRect_SpeedNum[0] = new RectF();
        destRect_SpeedNum[1] = new RectF();
        destRect_LevelNum = new RectF[2];
        destRect_LevelNum[0] = new RectF();
        destRect_LevelNum[1] = new RectF();
        // 建好2n个矩形
        sourceRect_Num = new Rect(0, 0, bmpNumbers[0].getWidth(), bmpNumbers[0].getHeight());
        destRect_Num = new RectF();
        sourceRect_Hi_Score = new Rect(0, 0, bmpHi_Score.getWidth(), bmpHi_Score.getHeight());
        destRect_Hi_Score = new RectF();
        sourceRect_Music = new Rect(0, 0, bmpMusic.getWidth(), bmpMusic.getHeight());
        destRect_Music = new RectF();
        sourceRect_SpeedLevel = new Rect(0, 0, bmpSpeedLevel.getWidth(), bmpSpeedLevel.getHeight());
        destRect_SpeedLevel = new RectF();
        destRect_Pause = new RectF();
        destRect_GameOver = new RectF();
        sourceRect_Pause = new Rect(0, 0, bmpPause.getWidth(), bmpPause.getHeight());
        sourceRect_GameOver = new Rect(0, 0, bmpGameOver.getWidth(), bmpGameOver.getHeight());
        destRect_16Bricks = new RectF[4][4];
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                destRect_16Bricks[i][j] = new RectF();
            }
        }
        sourceRect_16Bricks = new Rect(0, 0, bmpBrick.getWidth(), bmpBrick.getHeight());
        // 大小
        topNumbersX = new float[6];
    }

    // 绘制
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 原来根本就不用把原来恩位置清空!!! (＃°Д°)
        canvas.drawBitmap(bmpNumbers[getIOfNumber(score, 0)], sourceRect_Num, getTopNumber(5),
                paint);
        if (score >= 10)
            canvas.drawBitmap(bmpNumbers[getIOfNumber(score, 1)], sourceRect_Num, getTopNumber(4),
                    paint);
        if (score >= 100)
            canvas.drawBitmap(bmpNumbers[getIOfNumber(score, 2)], sourceRect_Num, getTopNumber(3),
                    paint);
        if (score >= 1000)
            canvas.drawBitmap(bmpNumbers[getIOfNumber(score, 3)], sourceRect_Num, getTopNumber(2),
                    paint);
        if (score >= 10000)
            canvas.drawBitmap(bmpNumbers[getIOfNumber(score, 4)], sourceRect_Num, getTopNumber(1),
                    paint);
        if (score >= 100000)
            canvas.drawBitmap(bmpNumbers[getIOfNumber(score, 5)], sourceRect_Num, getTopNumber(0),
                    paint);
        // 绘制Hi-Score最高分
        if (isHighScore)
            canvas.drawBitmap(bmpHi_Score, sourceRect_Hi_Score, destRect_Hi_Score, paint);
        // 绘制music音符是否启用
        if (soundEnabled) canvas.drawBitmap(bmpMusic, sourceRect_Music, destRect_Music, paint);
        // 绘制speedLevel
        canvas.drawBitmap(bmpSpeedLevel, sourceRect_SpeedLevel, destRect_SpeedLevel, paint);
        // 绘制speed & level的数字
        canvas.drawBitmap(bmpNumbers[getIOfNumber(speed, 0)], sourceRect_Num, destRect_SpeedNum[1],
                paint);
        if (speed > 9)
            canvas.drawBitmap(bmpNumbers[getIOfNumber(speed, 1)], sourceRect_Num,
                    destRect_SpeedNum[0], paint);
        if (level > 9)
            canvas.drawBitmap(bmpNumbers[getIOfNumber(level, 1)], sourceRect_Num,
                    destRect_LevelNum[0], paint);
        canvas.drawBitmap(bmpNumbers[getIOfNumber(level, 0)], sourceRect_Num,
                destRect_LevelNum[1], paint);
        if (paused)
            canvas.drawBitmap(bmpPause, sourceRect_Pause, destRect_Pause, paint);
        if (gameOverEnabled)
            canvas.drawBitmap(bmpGameOver, sourceRect_GameOver, destRect_GameOver, paint);
        if (preview != null)
            for (int y = 0; y < 4; y++)
                for (int x = 0; x < 4; ++x)
                    if (preview[x][y] != 0)
                        canvas.drawBitmap(bmpBrick, sourceRect_16Bricks, destRect_16Bricks[y][x], paint);
    }

    /**
     * 获取某个数字的第几位
     *
     * @param sourceNumber 源数字
     * @param i            第几位
     */
    private int getIOfNumber(int sourceNumber, int i) {
        if (i == 0)
            return sourceNumber % 10;
        else {
            return sourceNumber / getTen(i) % 10;
        }
    }

    /**
     * 获取数字10的几次幂
     */
    private int getTen(int i) {
        int n = 1;
        for (int j = 0; j < i; j++) {
            n *= 10;
        }
        return n;
    }

    private RectF getTopNumber(int i) {
        destRect_Num.left = topNumbersX[i];
        destRect_Num.top = topNumbersY;
        destRect_Num.right = destRect_Num.left + numberWidth;
        destRect_Num.bottom = destRect_Num.top + numberHeight;
        return destRect_Num;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        } else {
            width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300,
                    getResources().getDisplayMetrics());
        }
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
            height = MeasureSpec.getSize(heightMeasureSpec);
        } else {
            height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 375,
                    getResources().getDisplayMetrics());
        }
        if (height / width > 2f) {
            height = width * 10f / 3f;
        } else if (height / width < 2f) {
            width = height * 3f / 10f;
        }
        setMeasuredDimension((int) width, (int) height);
        initLocation();
    }

    private void initLocation() {
        topNumbersY = 0.0143575f * height;
        topNumbersX[0] = 0.070732f * width;
        topNumbersX[1] = 0.21463f * width;
        topNumbersX[2] = 0.3585f * width;
        topNumbersX[3] = 0.5049f * width;
        topNumbersX[4] = 0.6488f * width;
        topNumbersX[5] = 0.7927f * width;
        numberWidth = 0.104878f * width;
        numberHeight = 0.082555f * height;
        destRect_Hi_Score.set(0.05854f * width, 0.1407f * height, 0.680488f * width,
                0.177315f * height);
        destRect_Music.set(0.86585f * width, 0.1407f * height, 0.9561f * width, 0.17588f * height);
        destRect_SpeedLevel.set(0.104878f * width, 0.52692f * height, 0.887804f * width,
                0.563532f * height);
        destRect_SpeedNum[0].set(0.180488f * width, 0.394113f * height, 0.285366f * width,
                0.476669f * height);
        destRect_SpeedNum[1].set(0.304878f * width, 0.394113f * height, 0.409756f * width,
                0.476669f * height);
        destRect_LevelNum[0].set(0.590244f * width, 0.394113f * height, 0.695122f * width,
                0.476669f * height);
        destRect_LevelNum[1].set(0.717073f * width, 0.394113f * height, 0.821951f * width,
                0.476669f * height);
        destRect_Pause.set(0.304878f * width, 0.857143f * height, 0.719512f * width,
                0.885858f * height);
        destRect_GameOver.set(0.109756f * width, 0.912419f * height, 0.892683f * width,
                0.948313f * height);
        float start16X = 0.268293f * width;     // 起始X
        float start16Y = 0.208184f * height;    // 起始Y
        float brickWidth = 0.5f / 4.1f * width;
        float brickHeight = 0.5f / 13.93f * height;
        for (int i = 0; i < 4; ++i)
            for (int j = 0; j < 4; ++j) {
                destRect_16Bricks[i][j].set(start16X + j * brickWidth, start16Y + i * brickHeight,
                        start16X + (j + 1) * brickWidth, start16Y + (i + 1) * brickHeight);
            }
    }
}

package com.ykh.brickgames.myViews;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.ykh.brickgames.R;
import com.ykh.brickgames.network.BrickNetBroadcast;

/**
 * 主屏幕.
 */

public class GameScreen extends View {
    private float width;                        // 视图高度
    private float height;                       // 视图宽度
    private Paint paint;
    private Bitmap[] square;
    private float[] locationX = new float[10];      // 输入一个n返回X坐标
    private float[] locationY = new float[20];      // 输入一个n返回Y坐标
    private Rect sourceRect;
    private RectF destRect;
    private Adapter data;

    // 构造函数1
    public GameScreen(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    // 构造函数2
    public GameScreen(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    // 构造函数3
    public GameScreen(Context context) {
        super(context);
        init();
    }

    public void setData(Adapter value) {
        data = value;
        BrickNetBroadcast.data = value;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 从这里卡主发送数据
        for (int i = 0; i < 10; ++i)
            for (int j = 0; j < 20; ++j)
                canvas.drawBitmap(square[data.getScreenData(i, j)], sourceRect, getDestRect(i, j), paint);
        if (BrickNetBroadcast.enable) {
            BrickNetBroadcast.generate();
        }
    }

    private void init() {
        data = new Adapter() {
            @Override
            public int getScreenData(int x, int y) {
                return 10;
            }
        };
        BrickNetBroadcast.data = data;      // 提供给广播器使用
        BrickNetBroadcast.screen = this;    // 将屏幕提供给对方
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xff000000);
        square = new Bitmap[11];
        square[0] = BitmapFactory.decodeResource(getResources(), R.drawable.brick_0);
        square[1] = BitmapFactory.decodeResource(getResources(), R.drawable.brick_1);
        square[2] = BitmapFactory.decodeResource(getResources(), R.drawable.brick_2);
        square[3] = BitmapFactory.decodeResource(getResources(), R.drawable.brick_3);
        square[4] = BitmapFactory.decodeResource(getResources(), R.drawable.brick_4);
        square[5] = BitmapFactory.decodeResource(getResources(), R.drawable.brick_5);
        square[6] = BitmapFactory.decodeResource(getResources(), R.drawable.brick_6);
        square[7] = BitmapFactory.decodeResource(getResources(), R.drawable.brick_7);
        square[8] = BitmapFactory.decodeResource(getResources(), R.drawable.brick_8);
        square[9] = BitmapFactory.decodeResource(getResources(), R.drawable.brick_9);
        square[10] = BitmapFactory.decodeResource(getResources(), R.drawable.brick);
        sourceRect = new Rect(0, 0, square[0].getWidth(), square[0].getHeight());
        destRect = new RectF(0, 0, 0, 0);
    }

    private RectF getDestRect(int X, int Y) {
        destRect.set(locationX[X], locationY[Y], locationX[X] + locationX[1], locationY[Y] +
                locationX[1]);
        return destRect;
    }

    // 官方解释:
    // Measure the view and its content to determine the measured width and the measured height.
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
            height = width * 2f;
        } else if (height / width < 2f) {
            width = height / 2f;
        }
        setMeasuredDimension((int) width, (int) height);
        float w = width / 10f;
        float h = height / 20f;
        for (int i = 0; i < 10; ++i) {
            locationX[i] = w * i;
            locationY[i * 2] = h * i * 2f;
            locationY[i * 2 + 1] = h * (i * 2f + 1f);
        }
    }

    public interface Adapter {
        /**
         * 获得这里应该显示的方块
         *
         * @return 0-10分别代表10个透明度等级, 10为全黑
         */
        int getScreenData(int x, int y);
    }
}
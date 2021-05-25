package com.ykh.brickgames.myViews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.ykh.brickgames.Lg;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 四个控制按钮的View
 */

public class ControllerScreen extends View {
    private static String TAG = "ControllerView";
    private Paint whiteCirclePaint;
    private Paint orangeFillPaint, paintPressed;
    private Call call;
    private float Width, Height;      // 本View的宽和高
    private float centerX, centerY;   // 中心坐标
    private float A;                  // 宽度和高度最小值的一半
    private float radius;
    /**
     * 记录上一次操作
     * 数字说明 0: 无
     * 1: 左   2: 右
     * 3: 上   4: 下
     */
    private byte lastOpt = 0;
    private Timer timer;
    private Handler handler;
    private int count;

    public ControllerScreen(Context context) {
        super(context);
        init();
    }

    public ControllerScreen(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ControllerScreen(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setInterface(Call call) {
        this.call = call;
    }

    private void init() {
        orangeFillPaint = new Paint();
        orangeFillPaint.setColor(0xffff9900);
        orangeFillPaint.setStyle(Paint.Style.FILL);
        orangeFillPaint.setAntiAlias(true);
        paintPressed = new Paint();
        paintPressed.setStyle(Paint.Style.STROKE);              // Fill 和 Fill & Stroke 没有区别
        paintPressed.setStrokeWidth(2);
        paintPressed.setAntiAlias(true);                        // 抗锯齿
        paintPressed.setColor(0xffffffff);                      // 画笔设置为白色
//		paintPressed.setShadowLayer(半径, 偏移X, 偏移Y, 颜色);    // 设置阴影, 一点也不虚化
        whiteCirclePaint = new Paint();
        whiteCirclePaint.setStyle(Paint.Style.STROKE);
        whiteCirclePaint.setAntiAlias(true);
        whiteCirclePaint.setColor(0x00ffffff);
        createHandler();
        onResume();                                             // 开启时钟
    }

    @Override
    protected void onDraw(Canvas canvas) {
        DrawLeft(lastOpt == 1, canvas);
        DrawRight(lastOpt == 2, canvas);
        DrawTop(lastOpt == 3, canvas);
        DrawBottom(lastOpt == 4, canvas);
    }

    private void DrawLeft(boolean pressed, Canvas canvas) {
        float x = centerX - A + radius;
        float y = centerY;
        if (!pressed) {
            canvas.drawCircle(x, y, radius, orangeFillPaint);
            canvas.drawCircle(x, y, radius, whiteCirclePaint);
        } else {
            canvas.drawCircle(x, y, radius, paintPressed);
        }
    }

    private void DrawRight(boolean pressed, Canvas canvas) {
        float x = centerX + A - radius;
        float y = centerY;
        if (!pressed) {
            canvas.drawCircle(x, y, radius, orangeFillPaint);
        } else {
            canvas.drawCircle(x, y, radius, paintPressed);
        }
    }

    private void DrawTop(boolean pressed, Canvas canvas) {
        float x = centerX;
        float y = centerY - A + radius;
        if (!pressed) {
            canvas.drawCircle(x, y, radius, orangeFillPaint);
        } else {
            canvas.drawCircle(x, y, radius, paintPressed);
        }
    }

    private void DrawBottom(boolean pressed, Canvas canvas) {
        float x = centerX;
        float y = centerY + A - radius;
        if (!pressed) {
            canvas.drawCircle(x, y, radius, orangeFillPaint);
        } else {
            canvas.drawCircle(x, y, radius, paintPressed);
        }
    }

    private void createHandler() {
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (lastOpt) {
                    case 1:
                        call.onClickLeft();
                        break;
                    case 2:
                        call.onClickRight();
                        break;
                    case 3:
                        call.onClickUp();
                        break;
                    case 4:
                        call.onClickDown();
                        break;
                }
                return true;
            }
        });
    }

    // 发生触摸事件要反馈
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX() - centerX;
            float y = event.getY() - centerY;
            if (Math.abs(x) < A && Math.abs(y) < A) {
                // 如果触摸点到中心的距离是小于A的 (A是宽度&高度最小值的一般, 相当于半径)
                // 说明允许触发方向键
                if (x < 0 && Math.abs(x) > Math.abs(y)) {
                    if (lastOpt == 0) {
                        count = 0;
                        call.onClickLeft();
                        lastOpt = 1;
                    } else lastOpt = 0;
                } else if (x > 0 && Math.abs(x) > Math.abs(y)) {
                    if (lastOpt == 0) {
                        count = 0;
                        call.onClickRight();
                        lastOpt = 2;
                    } else lastOpt = 0;
                } else if (y < 0 && Math.abs(x) < Math.abs(y)) {
                    if (lastOpt == 0) {
                        count = 0;
                        call.onClickUp();
                        lastOpt = 3;
                    } else lastOpt = 0;
                } else if (y > 0 && Math.abs(x) < Math.abs(y)) {
                    if (lastOpt == 0) {
                        count = 0;
                        call.onClickDown();
                        lastOpt = 4;
                    } else lastOpt = 0;
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            lastOpt = 0;
        }
        invalidate();
        return true;
    }

    // 修改尺寸时调用, 另外检测一下MATCH_PARENT or WRAP_CONTENT
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 当宽度处于已给定尺寸时, 即非WRAP_CONTENT时
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            // WidthMeasure就是实际宽度
            Width = MeasureSpec.getSize(widthMeasureSpec);
        } else {
            Width = getPaddingLeft() + getPaddingRight();
        }
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
            Height = MeasureSpec.getSize(heightMeasureSpec);
        } else {
            Height = getPaddingTop() + getPaddingBottom();
        }
        // 提交设置好的宽度和高度
        setMeasuredDimension((int) Width, (int) Height);
        A = Math.min(Width, Height) / 2;
        centerX = Width / 2;
        centerY = Height / 2;
        radius = (int) (A * 0.4f);
    }

    /**
     * 终止: 销毁时钟
     */
    public void onDestroy() {
        onPause();
    }

    /**
     * 暂停: 销毁时钟
     */
    public void onPause() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * 恢复: 开始时钟
     */
    public void onResume() {
        if (timer == null) {
            timer = new Timer();
            timer.schedule(new KeyTimerTask(), 0, 100);             // 1秒发送10次信号
        }
    }

    public interface Call {
        void onClickLeft();

        void onClickRight();

        void onClickUp();

        void onClickDown();
    }

    private class KeyTimerTask extends TimerTask {
        @Override
        public void run() {
            if (count > 8)
                handler.sendEmptyMessage(0);
            else count++;
        }
    }
}
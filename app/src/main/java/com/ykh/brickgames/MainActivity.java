package com.ykh.brickgames;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.ykh.brickgames.games.Control;
import com.ykh.brickgames.myViews.ControllerScreen;
import com.ykh.brickgames.myViews.GameScreen;
import com.ykh.brickgames.myViews.ScoreScreen;
import com.ykh.brickgames.network.BrickNetBroadcast;
import com.ykh.brickgames.settings.PowerSetting;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private static byte SHOWING_ABOUT_DIALOG = 3;
    private static byte SHOWING_MENU = 2;
    private static byte SHOWING_NOTHING = 0;
    private static byte ACTIVITY_PAUSED = 1;

    private long timeLastPressBack = 0l;        // 上次点击back的时间
    private ImageView btnMusic;
    private ImageView btnStartOrPause;
    private ImageView btnOnOrOff;
    private ImageView btnReset; // 按顺序从左到右
    private Button btnBig;              // 最大的按钮, 其实叫旋转

    private PopupWindow popupWindow_menu;
    private PopupWindow popupWindow_dialog;
    private ScoreScreen scoreScreen;
    private GameScreen gameScreen;
    private ControllerScreen controller;
    private View popupView_menu;
    private View popupView_about;
    private View rootView;

    private Timer timerBigButton;
    private Handler handler;

    private Control mControl;
    private SoundPool soundPool;
    private PowerSetting powerSetting;
    private SparseIntArray map;
    private boolean soundEnabled = true;

    private byte pausefrom = SHOWING_NOTHING;

    // 放置按钮并直接设置onTouchListener
    // 方向键必须要是一个整体以便能够滑动
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = getLayoutInflater().inflate(R.layout.activity_main, null);
        setContentView(rootView);
        getViews();
        mControl = new Control(gameScreen, scoreScreen, this);
        setViews();
        initPopupWindow();
        timerBigButton = new Timer();       // 初始化计时器
        createHandler();                    // 创建Handler
        initSoundPool();
        powerSetting = new PowerSetting(this);

        // 使用透明状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 如果安卓版本大于4.4
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

    }

    private void createHandler() {
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == HandlerType.BigButton) {
                    onBigButtonClick();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        mControl.onDestroy();
        controller.onDestroy();

        super.onDestroy();
    }

    private void getViews() {
        btnMusic = (ImageView) findViewById(R.id.btnMusic);
        btnStartOrPause = (ImageView) findViewById(R.id.btnStartOrPause);
        btnOnOrOff = (ImageView) findViewById(R.id.btnMenu);
        btnReset = (ImageView) findViewById(R.id.btnReset);
        btnBig = (Button) findViewById(R.id.BigButton);
        controller = (ControllerScreen) findViewById(R.id.controller);
        scoreScreen = (ScoreScreen) findViewById(R.id.scoreScreen);
        gameScreen = (GameScreen) findViewById(R.id.gameScreen);

    }

    private void setViews() {
        controller.setInterface(mControl);
        ButtonStateListener listener = new ButtonStateListener();

        btnBig.setOnTouchListener(listener);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 如果按下了菜单键
        if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
            if (!popupWindow_menu.isShowing()) {
                if (pausefrom == SHOWING_NOTHING) {
                    mControl.onBigPause();
                    pausefrom = SHOWING_MENU;
                    showPopupWindow();
                }
            } else {
                popupWindow_menu.dismiss();
                if (pausefrom == SHOWING_MENU) {
                    mControl.onBigResume();
                    pausefrom = SHOWING_NOTHING;
                }
            }
        } else if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (popupWindow_menu.isShowing()) {
                popupWindow_menu.dismiss();
                if (pausefrom == SHOWING_MENU) {
                    mControl.onBigResume();
                    pausefrom = SHOWING_NOTHING;
                }
            } else {
                long t = System.currentTimeMillis();
                if (t - timeLastPressBack < 1000L) {
                    退出游戏();          // 结束Activity
                } else {
                    timeLastPressBack = t;      // 存储时间
                    Toast.makeText(this, "再次点击后退键结束程序", Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void 退出游戏() {
        finish();
    }

    private void initPopupView() {
        popupView_menu = LayoutInflater.from(this).inflate(R.layout.float_menu, null);
        popupView_about = LayoutInflater.from(this).inflate(R.layout.about, null);
        // TODO 处理里面各个按钮的单击事件

        MenuItemClickListener listener = new MenuItemClickListener();
        popupView_menu.findViewById(R.id.btnAbout).setOnClickListener(listener);
        popupView_menu.findViewById(R.id.btnExitApp).setOnClickListener(listener);
        popupView_menu.findViewById(R.id.btnCancelMenu).setOnClickListener(listener);
        popupView_menu.findViewById(R.id.btnScreenHoldOn).setOnClickListener(listener);
        popupView_menu.findViewById(R.id.btnServerOpen).setOnClickListener(listener);
        popupView_menu.findViewById(R.id.btnServer).setOnClickListener(listener);
        ((Button) popupView_menu.findViewById(R.id.btnServerOpen)).setText(BrickNetBroadcast.enable ? "投影服务器已打开" : "投影服务器已关闭");
        popupView_about.findViewById(R.id.btnDialogOk).setOnClickListener(listener);
        btnMusic.setOnClickListener(listener);
        btnOnOrOff.setOnClickListener(listener);
        btnStartOrPause.setOnClickListener(listener);
        btnReset.setOnClickListener(listener);
    }

    private void initPopupWindow() {
        initPopupView();
        popupWindow_menu = new PopupWindow(popupView_menu, ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT);
        popupWindow_menu.setBackgroundDrawable(getResources().getDrawable(
                android.R.drawable.screen_background_light_transparent));
        popupWindow_menu.setAnimationStyle(R.style.MenuAnimation);

//		popupWindow_menu.setOutsideTouchable(true);

        // 下面这个监听器在所有TouchListener中优先执行
//		popupWindow_menu.setTouchInterceptor(new View.OnTouchListener() {
//			@Override
//			public boolean onTouch(View v, MotionEvent event) {
//				return false;
//			}
//		});

        // 如果在显示状态更新的话需要调用
        // popupWindow_menu.update();
    }

    private void showAboutDialog() {
        popupWindow_dialog = new PopupWindow(popupView_about, ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT);
        popupWindow_dialog.setAnimationStyle(R.style.MenuAnimation);
        popupWindow_dialog.showAtLocation(rootView, Gravity.CENTER, 0, 0);
    }

    private void showPopupWindow() {
        ((Button) popupView_menu.findViewById(R.id.btnScreenHoldOn))
                .setText("屏幕常亮: " + (powerSetting.isEnabled() ? "开" : "关"));
        popupWindow_menu.showAtLocation(rootView, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
    }

    private void onBigButtonClick() {
//		scoreScreen.add(ScoreScreen.SCORE, 1);
        mControl.onClickRotate();
    }

    @Override
    protected void onPause() {
        if (timerBigButton != null) {
            timerBigButton.cancel();
            timerBigButton = null;
        }
        if (pausefrom == 0) {
            mControl.onBigPause();      // 大暂停
            pausefrom = ACTIVITY_PAUSED;
        }
        powerSetting.release();
        controller.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (pausefrom == ACTIVITY_PAUSED || pausefrom == SHOWING_ABOUT_DIALOG) {
            pausefrom = SHOWING_NOTHING;
            mControl.onBigResume();     // 大恢复
        }
        powerSetting.acquire();
        controller.onResume();
        super.onResume();
    }

    private void initSoundPool() {
        soundPool = new SoundPool(4/*同时播放的音效数量*/, AudioManager.STREAM_MUSIC, 0);
        map = new SparseIntArray();
        map.put(1, soundPool.load(this, R.raw.gun_shot, 1));
        map.put(2, soundPool.load(this, R.raw.water_di, 1));
    }

    public void playSound(int sound) {
/*        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);// 实例化
        float audioMaxVolum = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);// 音效最大值
        float audioCurrentVolum = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        float audioRatio = audioCurrentVolum / audioMaxVolum;*/
        Lg.e("系统通知:", "播放声音");
        if (soundEnabled)               // 只有声音启用的情况下播放
            soundPool.play(map.get(sound)/*声音ID*/, 1/*左声道音量*/, 1/*右声道音量*/,
                    1/*优先级*/, 1/*循环播放次数*/, 1/*播放速度, 0.5-2.0之间*/);
    }

    private static class HandlerType {
        static int BigButton = 1;
    }

    private class ButtonStateListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                timerBigButton = new Timer();
                timerBigButton.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        handler.sendEmptyMessage(HandlerType.BigButton);
                    }
                }, 0, 200);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                timerBigButton.cancel();
                timerBigButton = null;
            }
            return false;
        }
    }

    private class MenuItemClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnAbout:
                    popupWindow_menu.dismiss();
                    pausefrom = SHOWING_ABOUT_DIALOG;
                    showAboutDialog();
                    break;
                case R.id.btnExitApp:       // 退出程序
                    popupWindow_menu.dismiss();  //
                    退出游戏();
                    break;
                case R.id.btnCancelMenu:    // 取消菜单
                    if (pausefrom == SHOWING_MENU) {
                        pausefrom = 0;
                        mControl.onBigResume();
                    }
                    popupWindow_menu.dismiss();
                    break;
                case R.id.btnDialogOk:
                    popupWindow_dialog.dismiss();
                    if (pausefrom == SHOWING_ABOUT_DIALOG) {
                        pausefrom = 0;
                        mControl.onBigResume();
                    }
                    break;
                case R.id.btnMusic:
                    Lg.e("点击静音");   // 在抬起来的时候才算点击静音
                    soundEnabled = !soundEnabled;
                    scoreScreen.setSoundEnabled(soundEnabled);
                    break;
                case R.id.btnMenu:
                    Lg.e("点击开关");
                    if (pausefrom == 0) {
                        mControl.onBigPause();
                        pausefrom = SHOWING_MENU;
                    }
                    showPopupWindow();
                    break;
                case R.id.btnStartOrPause:
                    Lg.e("点击开始/暂停");
                    mControl.onClickPause();
                    break;
                case R.id.btnReset:
                    Lg.e("点击重新开始");
                    mControl.onClickReset();
                    break;
                case R.id.btnScreenHoldOn:
                    Lg.e("点击保持屏幕常亮");
                    powerSetting.setEnabled(!powerSetting.isEnabled());
                    ((Button) v).setText("屏幕常亮: " + (powerSetting.isEnabled() ? "开" : "关"));
                    break;
                case R.id.btnServerOpen:
                    Lg.e("主界面", "打开服务器");
                    if (!BrickNetBroadcast.enable) {
                        BrickNetBroadcast.onServerBroadcastSend();      // 打开
                        ((Button) v).setText(R.string.broadcast_open);
                    } else {
                        BrickNetBroadcast.enable = false;
                        ((Button) v).setText(R.string.broadcast_close);
                    }
                    break;
                case R.id.btnServer:
                    startActivity(new Intent(MainActivity.this, ServerActivity.class));
                    pausefrom = SHOWING_ABOUT_DIALOG;
                    popupWindow_menu.dismiss();
                    break;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadHiScore();
    }

    @Override
    protected void onStop() {
        saveHiScore();
        super.onStop();
    }

    private void saveHiScore() {
        SharedPreferences sharedPreferences = getSharedPreferences("score", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt("Score", Control.HI_SCORE);
        editor.apply();

    }

    private void loadHiScore() {
        SharedPreferences sharedPreferences = getSharedPreferences("score", MODE_PRIVATE);
        Control.HI_SCORE = sharedPreferences.getInt("Score", 0);
    }
}

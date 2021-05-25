package com.ykh.brickgames;

import android.util.Log;

/**
 * 日志输出
 */

public class Lg {
    private static final String TAG = "砖块游戏";
    private static boolean visible = true;

    public static void e(String msg) {
        if (visible)
            Log.e(TAG, msg);
    }

    public static void e(String tag, String msg) {
        if (visible)
            Log.e(TAG + "::" + tag, msg);
    }
}

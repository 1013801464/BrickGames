package com.ykh.brickgames.settings;

import android.content.Context;
import android.os.PowerManager;

/**
 * Created by user on 2017/2/13.
 */

public class PowerSetting {
    private final PowerManager.WakeLock wakeLock;
    PowerManager powerManager;
    /**
     * 是否启用屏幕常亮
     */
    private boolean enabled = false;

    public PowerSetting(Context context) {
        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "bg_screen_tag");
    }

    public void acquire() {
        if (enabled && !wakeLock.isHeld())
            wakeLock.acquire();
    }

    public void release() {
        if (wakeLock.isHeld())
            wakeLock.release();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        if (enabled != value) {
            if (!value) {                   // 如果是要关闭
                if (wakeLock.isHeld())      // 就关了
                    wakeLock.release();
            } else {                        // 如果是要打开
                if (!wakeLock.isHeld()) {   // 就打开
                    wakeLock.acquire();
                }
            }
            enabled = value;
        }
    }
}

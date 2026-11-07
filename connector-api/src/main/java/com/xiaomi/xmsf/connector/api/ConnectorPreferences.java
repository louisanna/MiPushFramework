package com.xiaomi.xmsf.connector.api;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 连接器自己的偏好读写，只覆盖连接器关心的少量开关，
 * 不依赖 :push 的 ConfigCenter。
 */
public final class ConnectorPreferences {
    private static final String NAME = "com.xiaomi.xmsf_preferences";
    private static final String KEY_FOREGROUND = "StartForegroundService";

    private final SharedPreferences preferences;

    public ConnectorPreferences(Context context) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(NAME, Context.MODE_MULTI_PROCESS);
    }

    public boolean isForegroundNotificationEnabled() {
        return preferences.getBoolean(KEY_FOREGROUND, false);
    }

    public void setForegroundNotificationEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_FOREGROUND, enabled).apply();
    }
}
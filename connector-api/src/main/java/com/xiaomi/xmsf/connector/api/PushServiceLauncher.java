package com.xiaomi.xmsf.connector.api;

import android.content.Context;
import android.content.Intent;

/** 由壳提供，用于把 Intent 转发给真正的推送服务实现。 */
public interface PushServiceLauncher {
    void launch(Context context, Intent intent);
}
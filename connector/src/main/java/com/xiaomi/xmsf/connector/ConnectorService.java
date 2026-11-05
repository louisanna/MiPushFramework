package com.xiaomi.xmsf.connector;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * L2 连接器占位服务：后续把 XMPushService / 收包 / 通知入口迁移到这里，
 * 运行在 :connector 进程（由壳的 manifest 指定 android:process）。
 */
public class ConnectorService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
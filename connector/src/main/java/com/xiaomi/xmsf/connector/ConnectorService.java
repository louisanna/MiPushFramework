package com.xiaomi.xmsf.connector;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/** L2 连接器占位服务：后续接收/转发逻辑迁移到这里。 */
public class ConnectorService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
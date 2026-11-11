package com.xiaomi.xmsf.connector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.os.Build;

/**
 * 连接器自己的前台服务实现：不依赖 :push 的 ForegroundHelper。
 * 被 startForegroundService 拉起时，必须在 5 秒内 startForeground。
 */
public final class ConnectorForeground {
    private static final String CHANNEL = "connector_status";
    private static final int NOTIFICATION_ID = 2;

    private ConnectorForeground() {
    }

    public static void ensure(Service service, boolean keepAlive) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager manager = service.getSystemService(NotificationManager.class);
                if (manager != null) {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL, "推送服务", NotificationManager.IMPORTANCE_MIN);
                    manager.createNotificationChannel(channel);
                }
            }
            Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? new Notification.Builder(service, CHANNEL)
                    : new Notification.Builder(service);
            builder.setSmallIcon(android.R.drawable.stat_notify_sync)
                    .setContentTitle("推送服务运行中")
                    .setOngoing(true);
            service.startForeground(NOTIFICATION_ID, builder.build());
            if (!keepAlive) {
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        service.stopForeground(Service.STOP_FOREGROUND_REMOVE);
                    } catch (Throwable ignored) {
                    }
                }, 30_000);
            }
        } catch (Throwable ignored) {
        }
    }
}
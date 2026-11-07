package com.xiaomi.xmsf.push.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * 由 ASM 注入到 SDK XMPushService.onStartCommand 开头，
 * 保证被 startForegroundService 拉起时 5 秒内调用 startForeground。
 * 完全自包含，不依赖 Global/ConfigCenter（:connector 进程里它们未初始化）。
 */
public final class FgsCompat {
    private static final String TAG = "FgsCompat";
    private static final String CHANNEL = "fgs_compat";
    private static final int ID = 3;

    private FgsCompat() {
    }

    public static void ensure(Service service) {
        try {
            NotificationManager manager = (NotificationManager)
                    service.getSystemService(Service.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL, "推送服务", NotificationManager.IMPORTANCE_MIN);
                manager.createNotificationChannel(channel);
            }
            Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? new Notification.Builder(service, CHANNEL)
                    : new Notification.Builder(service);
            builder.setSmallIcon(android.R.drawable.stat_notify_sync)
                    .setContentTitle("推送服务")
                    .setOngoing(true);
            service.startForeground(ID, builder.build());
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    service.stopForeground(Service.STOP_FOREGROUND_REMOVE);
                } catch (Throwable ignored) {
                }
            }, 30_000);
        } catch (Throwable e) {
            Log.e(TAG, "ensure foreground failed", e);
        }
    }
}

package com.xiaomi.xmsf.connector;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.xiaomi.xmsf.config.ConfigEnv;
import com.xiaomi.xmsf.push.utils.Configurations;
import com.xiaomi.xmsf.push.utils.IconConfigurations;

import top.trumeet.common.Constants;

/**
 * 连接器侧的推送服务桥：处理配置刷新并转发到 SDK 服务。
 * 原来在 :push 的 XMPushService bridge。
 */
public class ConnectorPushService extends IntentService {
    private static final String TAG = "ConnectorPushService";

    public ConnectorPushService() {
        super("ConnectorPushService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 可能被 startForegroundService 拉起，先满足 5 秒限制
        ConnectorForeground.ensure(this, false);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        if (Constants.CONFIGURATIONS_UPDATE_ACTION.equals(intent.getAction())) {
            try {
                Configurations.getInstance().init(this,
                        ConfigEnv.get().getConfigurationDirectory(this));
                IconConfigurations icons = com.nihility.utils.Singleton.instance();
                icons.init(this,
                        ConfigEnv.get().getConfigurationDirectory(this));
            } catch (Throwable e) {
                Log.e(TAG, "reload configurations failed", e);
            }
            return;
        }
        try {
            Intent forward = new Intent();
            forward.setComponent(
                    new ComponentName(this, com.xiaomi.push.service.XMPushService.class));
            forward.setAction(intent.getAction());
            forward.putExtras(intent);
            startService(forward);
            Log.d(TAG, "forward intent " + intent);
        } catch (Throwable e) {
            Log.e(TAG, "forward failed", e);
        }
    }
}
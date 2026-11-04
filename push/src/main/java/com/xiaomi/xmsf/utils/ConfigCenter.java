package com.xiaomi.xmsf.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.nihility.Global;
import com.xiaomi.xmsf.BuildConfig;
import com.xiaomi.xmsf.push.service.XMPushService;
import com.xiaomi.xmsf.push.utils.Configurations;

import top.trumeet.common.Constants;
import top.trumeet.common.utils.Utils;


/**
 * Push 配置
 * @author zts
 */
public class ConfigCenter {

    public ConfigCenter() {
    }

    //using MODE_MULTI_PROCESS emmm.....
    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", Context.MODE_MULTI_PROCESS);
    }

    /**
     * 目标应用已被卸载时，直接丢弃它的推送消息，不再展示通知。
     * 默认开启：应用卸载后不应该继续弹出它的推送（服务器侧的注册不会因此注销）。
     */
    public boolean isDropMessagesForUninstalledPackages(Context ctx) {
        return getSharedPreferences(ctx).getBoolean("DropMessagesForUninstalledPackages", true);
    }

    public boolean isNotificationOnRegister(Context ctx) {
        return getSharedPreferences(ctx).getBoolean("NotificationOnRegister", false);
    }

    /**
     * 收到推送时把框架界面拉到前台（需要“显示在其他应用上层”权限，否则会被系统拦截）。
     */
    public boolean isPullUpFrameworkOnPush(Context ctx) {
        return getSharedPreferences(ctx).getBoolean("PullUpFrameworkOnPush", false);
    }

    public boolean isShowConfigurationListOnLoaded(Context ctx) {
        return getSharedPreferences(ctx).getBoolean("ShowConfigurationListOnLoaded", false);
    }

    public int getAccessMode(Context ctx) {
        String mode = getSharedPreferences(ctx).getString("AccessMode", "0");
        return Integer.valueOf(mode);
    }

    public boolean isIceboxSupported(Context ctx) {
        return getSharedPreferences(ctx).getBoolean("IceboxSupported", false);
    }

    public Uri getConfigurationDirectory(Context ctx) {
        String uri = getSharedPreferences(ctx).getString("ConfigurationDirectory", null);
        return uri == null ? null : Uri.parse(uri);
    }

    public boolean setConfigurationDirectory(Context ctx, Uri treeUri) {
        return getSharedPreferences(ctx).edit()
                .putString("ConfigurationDirectory", treeUri.toString())
                .commit();
    }

    public String getXMPPServer(Context ctx) {
        return getSharedPreferences(ctx).getString("XMPP_server", null);
    }

    public boolean setXMPPServer(Context ctx, String host) {
        return getSharedPreferences(ctx).edit()
                .putString("XMPP_server", host)
                .commit();
    }

    public boolean isDebugMode() {
        return getSharedPreferences(Utils.getApplication()).getBoolean("DebugMode", false);
    }

    public boolean isShowAllEvents() {
        return getSharedPreferences(Utils.getApplication()).getBoolean("ShowAllEvents", false);
    }

    public boolean isStartForegroundService() {
        return getSharedPreferences(Utils.getApplication()).getBoolean("StartForegroundService", false);
    }

    public void loadConfigurations(Context context) {
        Configurations.getInstance().init(context,
                Global.ConfigCenter().getConfigurationDirectory(context));
        Global.IconConfigurations().init(context,
                Global.ConfigCenter().getConfigurationDirectory(context));
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context, XMPushService.class));
        intent.setAction(Constants.CONFIGURATIONS_UPDATE_ACTION);
        context.startService(intent);
    }
}

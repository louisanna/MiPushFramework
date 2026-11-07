package com.xiaomi.xmsf.config;

import android.content.Context;

import com.nihility.Global;

/** 由 :push 实现 :config 所需的环境能力。 */
public class PushConfigEnvironment implements ConfigEnvironment {
    @Override
    public android.net.Uri getConfigurationDirectory(Context context) {
        return Global.ConfigCenter().getConfigurationDirectory(context);
    }

    @Override
    public boolean isShowConfigurationListOnLoaded(Context context) {
        return Global.ConfigCenter().isShowConfigurationListOnLoaded(context);
    }

    @Override
    public Object convertConfigValue(Object root, String[] path, Object value) {
        return Global.ConfigValueConverter().convert(
                (com.xiaomi.xmpush.thrift.XmPushActionContainer) root, path, value);
    }
}

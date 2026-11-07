package com.xiaomi.xmsf.config;

import android.content.Context;
import android.net.Uri;

/** 由壳注入的环境能力，避免 :config 依赖 :push。 */
public interface ConfigEnvironment {
    Uri getConfigurationDirectory(Context context);

    boolean isShowConfigurationListOnLoaded(Context context);

    Object convertConfigValue(Object root, String[] path, Object value);
}
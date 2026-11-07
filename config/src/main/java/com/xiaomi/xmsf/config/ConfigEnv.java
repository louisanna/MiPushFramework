package com.xiaomi.xmsf.config;

/** 静态持有 {@link ConfigEnvironment}，由壳在启动时设置。 */
public final class ConfigEnv {
    private static volatile ConfigEnvironment environment;

    private ConfigEnv() {
    }

    public static void set(ConfigEnvironment env) {
        environment = env;
    }

    public static ConfigEnvironment get() {
        return environment;
    }
}
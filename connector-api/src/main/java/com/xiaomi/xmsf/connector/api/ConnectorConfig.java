package com.xiaomi.xmsf.connector.api;

/** 连接器需要的静态配置，由壳/UI 在启动时注入，避免反向依赖 :push。 */
public final class ConnectorConfig {
    private final boolean foregroundNotification;
    private final String processName;

    public ConnectorConfig(boolean foregroundNotification, String processName) {
        this.foregroundNotification = foregroundNotification;
        this.processName = processName;
    }

    public boolean isForegroundNotification() { return foregroundNotification; }

    public String getProcessName() { return processName; }
}
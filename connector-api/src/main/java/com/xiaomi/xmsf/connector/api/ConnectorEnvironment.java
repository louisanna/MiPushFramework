package com.xiaomi.xmsf.connector.api;

/** 连接器需要的运行期环境，由壳（:app/:push）注入。 */
public interface ConnectorEnvironment {
    /** 是否显示常驻通知（前台服务保活）。 */
    boolean isForegroundNotificationEnabled();

    /** 是否允许连接器在后台被拉起。 */
    boolean isBackgroundStartAllowed();

    ConnectorLogger logger();
}
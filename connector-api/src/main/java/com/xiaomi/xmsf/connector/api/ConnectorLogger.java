package com.xiaomi.xmsf.connector.api;

/** 连接器内部日志出口，避免直接依赖 :push 的 XLog 封装。 */
public interface ConnectorLogger {
    void d(String message);

    void w(String message, Throwable t);
}
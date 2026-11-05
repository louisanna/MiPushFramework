package com.xiaomi.xmsf.connector.api;

/** 连接器把收到的消息交给通知层实现的入口。 */
public interface PushMessageSink {
    /** @return true 表示已消费（例如已发通知） */
    boolean onMessage(String packageName, byte[] payload);
}
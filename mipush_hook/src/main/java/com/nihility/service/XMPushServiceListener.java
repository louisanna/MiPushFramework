package com.nihility.service;

import android.content.Intent;

/**
 * 监听器基类（原为 interface + default 方法）。
 * 改成抽象类后，D8 不再生成 default method 桥接，避免
 * AbstractMethodError: created() 这类产物不一致问题。
 */
public abstract class XMPushServiceListener {

    public void created() {
    }

    public void destroy() {
    }

    public void start(Intent intent) {
    }

    public enum ConnectionStatus {
        connecting,
        connected,
        disconnected;

        public static ConnectionStatus of(int i) {
            return values()[i];
        }
    }

    public void connectionStatusChanged(ConnectionStatus connectionStatus) {
    }
}

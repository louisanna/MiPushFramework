package com.nihility.service;

import com.xiaomi.push.service.XMPushServiceMessenger;

public class MessengerAbility extends XMPushServiceListener {
    private final XMPushServiceMessenger messenger;

    public MessengerAbility(XMPushServiceMessenger messenger) {
        this.messenger = messenger;
    }

    @Override
    public void connectionStatusChanged(ConnectionStatus connectionStatus) {
        messenger.notifyConnectionStatusChanged(connectionStatus.ordinal());
    }
}

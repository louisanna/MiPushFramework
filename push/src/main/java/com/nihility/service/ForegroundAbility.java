package com.nihility.service;

public class ForegroundAbility extends XMPushServiceListener {
    public final ForegroundHelper foregroundHelper;

    public ForegroundAbility(ForegroundHelper foregroundHelper) {
        this.foregroundHelper = foregroundHelper;
    }

    @Override
    public void created() {
        foregroundHelper.startForeground();
    }

    @Override
    public void destroy() {
        foregroundHelper.stopForegroundNotification();
    }
}

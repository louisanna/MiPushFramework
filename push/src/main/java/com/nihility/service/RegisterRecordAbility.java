package com.nihility.service;

import android.content.Intent;

public class RegisterRecordAbility extends XMPushServiceListener {
    private final RegisterRecorder registerRecorder;

    public RegisterRecordAbility(RegisterRecorder registerRecorder) {
        this.registerRecorder = registerRecorder;
    }

    @Override
    public void start(Intent intent) {
        registerRecorder.recordRegisterRequest(intent);
    }
}

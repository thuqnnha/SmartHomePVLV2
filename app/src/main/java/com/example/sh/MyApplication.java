package com.example.sh;

import android.app.Application;

import com.videogo.openapi.EZGlobalSDK;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Bật log
        EZGlobalSDK.showSDKLog(true);

        // Khởi tạo SDK với APP_KEY
        EZGlobalSDK.initLib(this, "200c7b837e244fb188daaa91e2f58a19");

        // Set access token
        //EZGlobalSDK.getInstance().setAccessToken("at.6vo8u5bubs71c66c5cdwsnyh19bok1qf-5c17wow9r0-19a7krx-appeulnhk");
    }
}

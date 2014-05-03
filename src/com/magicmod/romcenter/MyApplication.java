package com.magicmod.romcenter;

import android.app.Application;

import com.magicmod.romcenter.utils.Constants;

import cn.jpush.android.api.JPushInterface;

public class MyApplication extends Application{
    private static final String TAG = "MyApplication";
    private static final boolean DBG =Constants.DEBUG;
    @Override
    public void onCreate() {
        super.onCreate();
        if (DBG) {
            JPushInterface.setDebugMode(true);
        } else {
            JPushInterface.setDebugMode(false);
        }
        JPushInterface.init(this);
    }
}

package com.gpuimage.appdemo;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import com.gpuimage.appdemo.model.AppDirectoryHelper;

/**
 * @ClassName
 * @Description
 * @Author danny
 * @Date 2017/12/20 11:19
 */
public class BaseApplication extends Application {
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    public static Context getAppContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        AppDirectoryHelper.checkSdcardFolder();
    }
}

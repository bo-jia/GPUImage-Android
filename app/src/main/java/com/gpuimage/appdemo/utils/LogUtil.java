package com.gpuimage.appdemo.utils;

import android.util.Log;

public class LogUtil {
    public static void w(String tag, String str) {
        Log.w(tag, str);
    }

    public static void d(String tag, String str) {
        Log.d(tag, str);
    }

    public static void v(String tag, String str) {
        Log.v(tag, str);
    }

    public static void i(String tag, String str) {
        Log.i(tag, str);
    }

    public static void e(String tag, String str) {
        Log.e(tag, str);
    }

    public static void e(String tag, Object o) {
        Log.e(tag, "", (Throwable) o);
    }
}


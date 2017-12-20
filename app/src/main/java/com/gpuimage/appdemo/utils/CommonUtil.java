package com.gpuimage.appdemo.utils;

import android.content.Context;

import com.gpuimage.appdemo.BaseApplication;

import java.io.InputStream;

/**
 * @ClassName
 * @Description
 * @Author danny
 * @Date 2017/12/20 18:58
 */
public class CommonUtil {
    protected static final String TAG = "CommonUtil";

    public static boolean copyAssetsResToSD(String url, String dstPath) {
        try {
            InputStream inputStream = BaseApplication.getAppContext().getAssets().open(url);
            FileUtils.writeToFile(inputStream, dstPath);
            return true;
        } catch (Exception e) {
            LogUtil.e(TAG, e);
        }
        return false;
    }

    public static boolean copyRawResourceToSD(Context context, int resourceId, String dstPath) {
        try {
            InputStream inputStream = context.getResources().openRawResource(resourceId);
            FileUtils.writeToFile(inputStream, dstPath);
            return true;
        } catch (Exception e) {
            LogUtil.e("Failed to export resource "
                    + ". Exception thrown: " , e);
        }
        return false;
    }
}

package com.gpuimage.appdemo.model;


import com.gpuimage.appdemo.BaseApplication;
import com.gpuimage.appdemo.utils.FileUtils;
import com.gpuimage.appdemo.utils.LogUtil;
import com.gpuimage.appdemo.utils.StorageUtil;

import java.io.File;
import java.util.ArrayList;

/**
 * 获取APP的基础目录信息
 * @author danny
 */
public class AppDirectoryHelper {
    private static final String TAG = "AppDirectoryHelper";

    private static final String SYSTEM_SEPARATOR = System.getProperty("file.separator");

    private static final String DIR_IMAGES_CACHE = "images_cache";
    private static final String DIR_HTTP_CACHE = "http_cache";

    //获取image cache路径
    public static String getImageCachePath() {
        return getCachePath(DIR_IMAGES_CACHE);
    }

    //获取http cache路径
    public static String getHttpCachePath() {
        return getCachePath(DIR_HTTP_CACHE);
    }

    private static String getCachePath(String dirname) {
        StringBuilder sb = new StringBuilder();
        sb.append(StorageUtil.getCacheDirectory(BaseApplication.getAppContext(),true));
        sb.append(SYSTEM_SEPARATOR);
        sb.append(dirname);
        if (FileUtils.createDirectoryAtPath(sb.toString())) {
            return sb.toString();
        } else {
            sb = new StringBuilder();
            String cacheDirPath = BaseApplication.getAppContext().getCacheDir().getAbsolutePath();
            sb.append(cacheDirPath);
            sb.append(SYSTEM_SEPARATOR);
            sb.append(dirname);
            if (FileUtils.createDirectoryAtPath(sb.toString())) {
                return sb.toString();
            } else {
                LogUtil.e(TAG, "getCachePath error,  " + sb.toString());
                return "";
            }
        }
    }

    /**
     * 检查目录情况 生成应用需要的文件结构。
     */
    public static void checkSdcardFolder() {
        ArrayList<File> dirList = new ArrayList<>();
        dirList.add(new File(getImageCachePath()));
        dirList.add(new File(getHttpCachePath()));

        for (File file : dirList) {
            if (FileUtils.isDirectory(file) && (file.exists())) {
                LogUtil.d(TAG, file.getAbsolutePath() + " is exist");
            } else {
                FileUtils.createNewDirectory(file);
            }
        }
    }
}

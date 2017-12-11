package com.gpuimage;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by j on 28/11/2017.
 */

public class GLog {
    public final static String TAG = "GPUImage";

    public static void v(String str) {
        Log.v(TAG, str);
    }

    public static void i(String str) {
        Log.i(TAG, str);
    }

    public static void e(String str) {
        Log.e(TAG, str);
    }

    public static void a(boolean assertion, String str) {
        if (!assertion) e(str);
        assert(assertion);
    }

    public static void checkFramebufferStatus() {
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        a(status == GLES20.GL_FRAMEBUFFER_COMPLETE, "Incomplete filter FBO: " + status);
    }

    public static void checkStatus(String info) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, info + " " + getErrorInfo(error));
        }
    }

    private static String getErrorInfo(int error) {
        switch(error) {
            case GLES20.GL_NO_ERROR:
                return "GL_NO_ERROR";
            case GLES20.GL_INVALID_ENUM:
                return "GL_INVALID_ENUM";
            case GLES20.GL_INVALID_VALUE:
                return "GL_INVALID_VALUE";
            case GLES20.GL_INVALID_OPERATION:
                return "GL_INVALID_OPERATION";
            case GLES20.GL_OUT_OF_MEMORY:
                return "GL_OUT_OF_MEMORY";
            default:
                return "unknown error: " + error;
        }
    }

    public static void writeBitmap(Bitmap bitmap, String name) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/gpuimage/" + name+".png";
        File dir = new File(path);
        if (!dir.exists()) dir.mkdirs();

        File f= new File(path);
        if (f.exists()) f.delete();
        try {
            if (f.createNewFile()) {
                FileOutputStream fos = new FileOutputStream(f);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


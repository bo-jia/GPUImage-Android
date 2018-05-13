package com.gpuimage.cvutils;

import android.graphics.Bitmap;

import com.gpuimage.GPUImageUtils;

import java.nio.ByteBuffer;

/**
 * Created by j on 11/5/2018.
 */

public class CVPicture {
    public final static int RGBA = 0;

    private int mWidth, mHeight;
    private ByteBuffer mHandler = null;

    public CVPicture(String path, int maxSize) {
        int[] size = new int[2];
        mHandler = CVImageUtils.ReadImage(path, size, maxSize);
        mWidth  = size[0];
        mHeight = size[1];
    }

    public CVPicture(String path) {
        this(path, -1);
    }

    public void getData(byte[] data, int scaledWidth, int scaledHeight) {
        CVImageUtils.GetData(mHandler, mWidth, mHeight, data, scaledWidth, scaledHeight);
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public ByteBuffer getHandler() {
        return mHandler;
    }

    public void release() {
        if (mHandler != null) {
            CVImageUtils.ReleaseImage(mHandler);
            mHandler = null;
        }
    }
}

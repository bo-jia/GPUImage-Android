package com.gpuimage.cvutils;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;

/**
 * Created by Felix on 17/10/2017.
 */

public class CVImageUtils {
    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("cvimageutils");
    }
    // src data is copied from bitmap
    public static native void NV122RGBAEla(byte[] src, int width, int height, int[] crop, int stride, int slice_height, byte[] dst);
    public static native void NV122YUVEla(byte[] src, int width, int height, int[] crop, int stride, int slice_height, byte[] dst);
    public static native void CompactNV12Ela(byte[] src, int width, int height, int[] crop, int stride, int slice_height, byte[] dst);
    public static native void YUV2RGBA(byte[] src, int width, int height, byte[] dst);
    public static native void NV122RGBA(byte[] src, int width, int height, byte[] dst);
    public static native void NV122RGBA2(byte[] src, int sw, int sh, byte[] dst, int dw, int dh);
    public static native void Gray2RGBA(byte[] src, int width, int height, int offset, byte[] dst);
    public static native void ConvertToSemiPlanar(byte[] yuv, int width, int height);
    public static native void RotateClockwise(byte[] yuv, int width, int height, int rotation);

    public static native ByteBuffer ReadImage(String path, int[] size, int maxSize);
    public static native void GetData(ByteBuffer handler, int sw, int sh, byte[] data, int dw, int dh);
    public static native void ReleaseImage(ByteBuffer handler);

    public static native void LoadBitmapForTexture(Bitmap bitmap, int texWidth, int texHeight);
    public static native void LoadCVPictureForTexture(ByteBuffer picHandler, int picWidth, int picHeight, int texWidth, int texHeight);
}

package com.gpuimage.cvutils;

/**
 * Created by Felix on 17/10/2017.
 */

public class CVImageUtils {
    static {
        System.loadLibrary("cvimageutils");
    }
    // src data is copied from bitmap
    public static native void NV122RGBAEla(byte[] src, int width, int height, int[] crop, int stride, int slice_height, byte[] dst);
    public static native void NV122YUVEla(byte[] src, int width, int height, int[] crop, int stride, int slice_height, byte[] dst);
    public static native void CompactNV12Ela(byte[] src, int width, int height, int[] crop, int stride, int slice_height, byte[] dst);
    public static native void YUV2RGBA(byte[] src, int width, int height, byte[] dst);
    public static native void NV122RGBA(byte[] src, int width, int height, byte[] dst);
    public static native void Gray2RGBA(byte[] src, int width, int height, int offset, byte[] dst);
}

package com.gpuimage.mediautils;

import java.nio.ByteBuffer;

/**
 * Created by j on 15/4/2018.
 */

public class GByteArrayUtils {
    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("cvimageutils");
    }

    public static native ByteBuffer newByteArray(int size);
    public static native void setBytes(ByteBuffer dst, byte[] src, int offset, int length);
    public static native void getBytes(byte[] dst, int offset, int length, ByteBuffer src);
    public static native void freeByteArray(ByteBuffer handler);

}

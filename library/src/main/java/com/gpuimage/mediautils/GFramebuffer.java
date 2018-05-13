package com.gpuimage.mediautils;

import java.nio.ByteBuffer;

/**
 * Created by j on 15/4/2018.
 */

public class GFramebuffer {
    public int width        = 0;
    public int height       = 0;
    public int length       = 0;
    public double timestamp = 0;

    private ByteBuffer mByteArray = null;

    public GFramebuffer(int length) {
        assert (length > 0);
        this.length = length;
        mByteArray  = GByteArrayUtils.newByteArray(length);
    }

    public void setData(byte[] data) {
        assert (data != null && data.length < length);
        this.length = data.length;
        GByteArrayUtils.setBytes(mByteArray, data, 0, data.length);
    }

    public void setData(byte[] data, int width, int height, double timestamp) {
        assert (data != null && data.length < length);
        this.width      = width;
        this.height     = height;
        this.length     = data.length;
        this.timestamp  = timestamp;
        GByteArrayUtils.setBytes(mByteArray, data, 0, data.length);
    }

    public byte[] getData(byte[] data) {
        assert (length > 0);
        if (data == null) {
            data = new byte[this.length];
        }
        GByteArrayUtils.getBytes(data, 0, length, mByteArray);
        return data;
    }

    public void release() {
        if (mByteArray != null) {
            GByteArrayUtils.freeByteArray(mByteArray);
        }
        mByteArray = null;
    }
}

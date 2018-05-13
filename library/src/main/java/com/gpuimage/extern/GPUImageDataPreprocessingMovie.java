package com.gpuimage.extern;

import com.gpuimage.GDispatchQueue;
import com.gpuimage.GLog;
import com.gpuimage.GSize;
import com.gpuimage.cvutils.CVImageUtils;
import com.gpuimage.sources.GPUImageMovie;

/**
 * Created by j on 18/12/2017.
 */

public class GPUImageDataPreprocessingMovie extends GPUImageMovie implements GPUImageDataPreprocessing {

    public final static int kOnlineDataSize = 256;

    private PreprocessingListener mPreprocessingListener;
    private byte[] mRgba;
    private byte[] mLastFrameYuvData;
    private GSize  mLastFrameSize = new GSize();
    private double mLastFrameTimestamp;
    @Override
    public void setPreprocessingListener(PreprocessingListener listener) {
        mPreprocessingListener = listener;
    }

    @Override
    public void processMovieFrame(byte[] yuvData, int width, int height, double frameTime) {
        if (mPreprocessingListener != null) {

            int dw = width  * kOnlineDataSize / Math.max(width, height);
            int dh = height * kOnlineDataSize / Math.max(width, height);
            dw = (dw >> 1) << 1;
            dh = (dh >> 1) << 1;

            if (mRgba == null || mRgba.length != dw * dh * 4) {
                mRgba = new byte[dw * dh * 4];
            }
            CVImageUtils.NV122RGBA2(yuvData, width, height, mRgba, dw, dh);
            mPreprocessingListener.preprocess(mRgba, dw, dh, frameTime);
        }
        mLastFrameYuvData     = yuvData;
        mLastFrameSize.width  = width;
        mLastFrameSize.height = height;
        mLastFrameTimestamp   = frameTime;
        super.processMovieFrame(yuvData, width, height, frameTime);
    }

    @Override
    public void refreshLastFrame() {
        if (mLastFrameYuvData != null) {
            processMovieFrame(mLastFrameYuvData, mLastFrameSize.width, mLastFrameSize.height, mLastFrameTimestamp);
        }
    }
}

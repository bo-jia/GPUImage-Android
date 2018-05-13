package com.gpuimage.extern;

import android.graphics.Bitmap;

import com.gpuimage.GDispatchQueue;
import com.gpuimage.GPUImagePicture;
import com.gpuimage.GSize;
import com.gpuimage.cvutils.CVImageUtils;
import com.gpuimage.cvutils.CVPicture;

/**
 * Created by j on 18/12/2017.
 */

public class GPUImageDataPreprocessingPicture extends GPUImagePicture implements GPUImageDataPreprocessing {

    private byte[] mRgba;
    private GSize  mRgbaSize = new GSize();
    private PreprocessingListener mPreprocessingListener;

    public GPUImageDataPreprocessingPicture(CVPicture picture, boolean smoothlyScaleOutput, boolean releasePicture) {
        super(picture, smoothlyScaleOutput, false);

        int width  = picture.getWidth();
        int height = picture.getHeight();
        int dw = width  * GPUImageDataPreprocessingMovie.kOnlineDataSize / Math.max(width, height);
        int dh = height * GPUImageDataPreprocessingMovie.kOnlineDataSize / Math.max(width, height);
        dw = (dw >> 1) << 1;
        dh = (dh >> 1) << 1;

        if (mRgba == null || mRgba.length != dw * dh * 4) {
            mRgba = new byte[dw * dh * 4];
        }
        picture.getData(mRgba, dw, dh);
        mRgbaSize.width  = dw;
        mRgbaSize.height = dh;

        GDispatchQueue.runSynchronouslyOnVideoProcessingQueue(()->{
            if (releasePicture) {
                picture.release();
            }
        });
    }

    @Override
    public void processImage() {
        if (mPreprocessingListener != null && mRgba != null) {
            mPreprocessingListener.preprocess(mRgba, mRgbaSize.width, mRgbaSize.height, 0);
        }
        super.processImage();
    }

    @Override
    public void setPreprocessingListener(PreprocessingListener listener) {
        mPreprocessingListener = listener;
    }

    @Override
    public void refreshLastFrame() {
        processImage();
    }
}

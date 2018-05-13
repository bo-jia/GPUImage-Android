package com.gpuimage.extern;

/**
 * Created by j on 18/12/2017.
 */

public interface GPUImageDataPreprocessing {
    interface PreprocessingListener {
        void preprocess(byte[] image, int width, int height, double timestamp);
    }
    void setPreprocessingListener(PreprocessingListener listener);
    void refreshLastFrame();
}

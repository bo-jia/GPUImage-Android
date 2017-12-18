package com.gpuimage.outputs;

import android.view.Surface;

import com.gpuimage.GPUImageContext;
import com.gpuimage.GPUImageFramebuffer;
import com.gpuimage.GPUImageInput;
import com.gpuimage.GSize;
import com.gpuimage.mediautils.GMediaMP4Writer;

import java.io.File;
import java.io.IOException;

import javax.microedition.khronos.egl.EGLSurface;

/**
 * Created by j on 16/12/2017.
 */

public class GPUImageWriter implements GPUImageInput {

    private GMediaMP4Writer mWriter;
    private File mOutputFile;
    private EGLSurface mEGLSurface;


    public GPUImageWriter(int width, int height, String path) {
        try {
            mOutputFile = new File(path);
            mWriter = new GMediaMP4Writer(width, height, mOutputFile, null);

//            mWriterSurface = mWriter.getInputSurface();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setInputSize(GSize newSize, int textureIndex) {

    }

    @Override
    public void newFrameReadyAtTime(double frameTime, int texIndex) {

    }

    @Override
    public void setInputFramebuffer(GPUImageFramebuffer newInputFramebuffer, int texIndex) {

    }

    @Override
    public void setInputRotation(GPUImageContext.GPUImageRotationMode newInputRotation, int textureIndex) {

    }

    @Override
    public GSize maximumOutputSize() {
        return null;
    }

    @Override
    public void endProcessing() {

    }

    @Override
    public boolean shouldIgnoreUpdatesToThisTarget() {
        return false;
    }

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public boolean wantsMonochromeInput() {
        return false;
    }

    @Override
    public void setCurrentlyReceivingMonochromeInput(boolean newValue) {

    }

    @Override
    public int nextAvailableTextureIndex() {
        return 0;
    }
}

package com.gpuimage;

import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by j on 28/11/2017.
 */

public class GPUImageRenderer implements GLSurfaceView.Renderer {
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GPUImageContext.sharedContext().getParameters();
        GPUImageProcessingQueue.sharedQueue().setThreadID(Thread.currentThread().getId());
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

    }

    @Override
    public void onDrawFrame(GL10 gl) {

    }
}

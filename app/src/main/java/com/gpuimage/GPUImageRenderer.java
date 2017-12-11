package com.gpuimage;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.gpuimage.sources.GPUImageOutput;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by j on 28/11/2017.
 */

public class GPUImageRenderer implements GLSurfaceView.Renderer {

    GPUImageFramebuffer mRendererFramebuffer = null;
    private GLProgram mRendererProgram;
    private int mRendererPositionAttribute, mRendererTextureCoordinateAttribute, mRendererInputTextureUniform;
    private ByteBuffer mVerticesCoordBuffer, mTextureCoordBuffer;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GPUImageContext.sharedContext().getParameters();
        GPUImageProcessingQueue.sharedQueue().setThreadID(Thread.currentThread().getId());
        initDisplayProgram();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (mRendererFramebuffer != null) {
            mRendererFramebuffer.unlock();
        }
        mRendererFramebuffer = GPUImageContext.sharedContext().framebufferCache.fetchFramebuffer(new GSize(width, height), false);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GPUImageProcessingQueue.sharedQueue().execute();
        presentFramebuffer();
    }

    public GPUImageFramebuffer rendererBuffer() {
        return mRendererFramebuffer;
    }

    private void presentFramebuffer() {
        GPUImageContext.sharedContext().setActiveShaderProgram(mRendererProgram);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glViewport(0, 0, mRendererFramebuffer.size().width, mRendererFramebuffer.size().height);

        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mRendererFramebuffer.texture());
        GLES20.glUniform1i(mRendererInputTextureUniform, 4);

        GLES20.glVertexAttribPointer(mRendererPositionAttribute, 2, GLES20.GL_FLOAT, false,0 , mVerticesCoordBuffer);
        GLES20.glVertexAttribPointer(mRendererTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, mTextureCoordBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private void initDisplayProgram() {
        mRendererProgram = GPUImageContext.sharedContext().programForShaders(GPUImageFilter.kGPUImageVertexShaderString, GPUImageFilter.kGPUImagePassthroughFragmentShaderString);
        if (!mRendererProgram.initialized) {
            if (!mRendererProgram.link()) {
                String progLog = mRendererProgram.programLog;
                GLog.e("Program link log: " + progLog);
                String fragLog = mRendererProgram.fragmentShaderLog;
                GLog.e("Fragment shader compile log: " + fragLog);
                String vertLog = mRendererProgram.vertexShaderLog;
                GLog.e("Vertex shader compile log: " + vertLog);
                mRendererProgram = null;
                GLog.a(false, "Filter shader link failed");
            }
        }
        GPUImageContext.sharedContext().setActiveShaderProgram(mRendererProgram);
        mRendererPositionAttribute = mRendererProgram.attributeIndex("position");
        mRendererTextureCoordinateAttribute = mRendererProgram.attributeIndex("inputTextureCoordinate");
        mRendererInputTextureUniform = mRendererProgram.uniformIndex("inputImageTexture");

        GLES20.glEnableVertexAttribArray(mRendererPositionAttribute);
        GLES20.glEnableVertexAttribArray(mRendererTextureCoordinateAttribute);

        mVerticesCoordBuffer = GPUImageOutput.FillNativeBuffer(mVerticesCoordBuffer, GPUImageFilter.imageVertices);
        mTextureCoordBuffer = GPUImageOutput.FillNativeBuffer(mTextureCoordBuffer, GPUImageFilter.textureCoordinatesForRotation(GPUImageContext.GPUImageRotationMode.kGPUImageNoRotation));
    }
}

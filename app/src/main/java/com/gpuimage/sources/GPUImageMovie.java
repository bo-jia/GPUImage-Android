package com.gpuimage.sources;

import android.opengl.GLES20;

import com.gpuimage.GLProgram;
import com.gpuimage.GLog;
import com.gpuimage.GPUImageContext;
import com.gpuimage.GPUImageFilter;
import com.gpuimage.GPUImageInput;
import com.gpuimage.GPUImageProcessingQueue;
import com.gpuimage.GSize;

import java.nio.FloatBuffer;

/**
 * Created by j on 4/12/2017.
 */

public class GPUImageMovie extends GPUImageOutput {
    private double mProcessingFrameTime;
    private GLProgram mYuvConversionProgram;

    private int mYuvConversionPositionAttribute, mYuvConversionTextureCoordinateAttribute;
    private int mYuvConversionLuminanceTextureUniform, mYuvConversionChrominanceTextureUniform;
    private int mYuvConversionMatrixUniform;

    private int mLuminanceTexture, mChrominanceTexture;
    private GSize mFrameSize = null;

    private float[] mPreferredConversion;

    private final static float mSquareVertices[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f,  1.0f,
            1.0f,  1.0f,
    };
    private final static float mTextureCoordinates[] = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
    };

    public GPUImageMovie() {
        yuvConversionSetup();
    }

    private void yuvConversionSetup() {
        GPUImageProcessingQueue.sharedQueue().runSyn(new Runnable() {
            @Override
            public void run() {
                mYuvConversionProgram = GPUImageContext.sharedContext().programForShaders(
                        GPUImageFilter.kGPUImageVertexShaderString,
                        GPUImageColorConversion.kGPUImageYUVFullRangeConversionForLAFragmentShaderString);
                if (!mYuvConversionProgram.initialized) {
                    if (!mYuvConversionProgram.link()) {
                        String progLog = mYuvConversionProgram.programLog;
                        GLog.e("Program link log: " + progLog);
                        String fragLog = mYuvConversionProgram.fragmentShaderLog;
                        GLog.e("Fragment shader compile log: " + fragLog);
                        String vertLog = mYuvConversionProgram.vertexShaderLog;
                        GLog.e("Vertex shader compile log: " + vertLog);
                        mYuvConversionProgram = null;
                        GLog.a(false, "Filter shader link failed");
                    }
                }

                mYuvConversionPositionAttribute = mYuvConversionProgram.attributeIndex("position");
                mYuvConversionTextureCoordinateAttribute = mYuvConversionProgram.attributeIndex("inputTextureCoordinate");
                mYuvConversionLuminanceTextureUniform = mYuvConversionProgram.uniformIndex("luminanceTexture");
                mYuvConversionChrominanceTextureUniform = mYuvConversionProgram.uniformIndex("chrominanceTexture");
                mYuvConversionMatrixUniform = mYuvConversionProgram.uniformIndex("colorConversionMatrix");

                GPUImageContext.sharedContext().setActiveShaderProgram(mYuvConversionProgram);

                GLES20.glEnableVertexAttribArray(mYuvConversionPositionAttribute);
                GLES20.glEnableVertexAttribArray(mYuvConversionTextureCoordinateAttribute);
            }
        });
    }

    private void checkTexture(int width, int height) {
        if (mFrameSize != null || mFrameSize.width != width || mFrameSize.height != height) {
            if (mLuminanceTexture > 0) {
                GLES20.glDeleteTextures(1, new int[] {mLuminanceTexture}, 0);
            }
            if (mChrominanceTexture > 0) {
                GLES20.glDeleteTextures(1, new int[] {mChrominanceTexture}, 0);
            }

            mFrameSize = new GSize(width, height);

            int tex[] = new int[2];
            GLES20.glGenTextures(2, tex, 0);
            mLuminanceTexture = tex[0];
            mChrominanceTexture = tex[1];

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mLuminanceTexture);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, width, height, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
                    null);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mChrominanceTexture);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA, width, height, 0, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE,
                    null);
        }
    }

    public void processMovieFrame(byte[] yuvData, int width, int height, double frameTime) {

        mProcessingFrameTime = frameTime;
        mPreferredConversion = GPUImageColorConversion.kColorConversion601FullRangeDefault;

        checkTexture(width, height);

        convertYUVToRGBOutput();

        for (GPUImageInput currentTarget : mTargets) {
            int indexOfObject = mTargets.indexOf(currentTarget);
            int targetTextureIndex = mTargetTextureIndices.get(indexOfObject);

            currentTarget.setInputSize(mFrameSize, targetTextureIndex);
            currentTarget.setInputFramebuffer(mOutputFramebuffer, targetTextureIndex);
        }

        mOutputFramebuffer.unlock();

        for (GPUImageInput currentTarget : mTargets) {
            int indexOfObject = mTargets.indexOf(currentTarget);
            int targetTextureIndex = mTargetTextureIndices.get(indexOfObject);
            currentTarget.newFrameReadyAtTime(frameTime, targetTextureIndex);
        }
    }

    public void endProcessing() {
        for (GPUImageInput currentTarget : mTargets) {
            currentTarget.endProcessing();
        }
    }

    private void convertYUVToRGBOutput() {
        GPUImageContext.sharedContext().setActiveShaderProgram(mYuvConversionProgram);
        mOutputFramebuffer = GPUImageContext.sharedContext().framebufferCache.fetchFramebuffer(mFrameSize, false);
        mOutputFramebuffer.activateFramebuffer();

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mLuminanceTexture);
        GLES20.glUniform1i(mYuvConversionLuminanceTextureUniform, 4);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mChrominanceTexture);
        GLES20.glUniform1i(mYuvConversionChrominanceTextureUniform, 5);

        GLES20.glUniformMatrix3fv(mYuvConversionMatrixUniform, 1, false, FloatBuffer.wrap(mPreferredConversion));

        GLES20.glVertexAttribPointer(mYuvConversionPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, FloatBuffer.wrap(mSquareVertices));
        GLES20.glVertexAttribPointer(mYuvConversionTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, FloatBuffer.wrap(mTextureCoordinates));

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }
}

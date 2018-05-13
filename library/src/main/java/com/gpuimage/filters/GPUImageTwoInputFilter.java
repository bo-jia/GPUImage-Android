package com.gpuimage.filters;

import android.opengl.GLES20;

import com.gpuimage.GDispatchQueue;
import com.gpuimage.GPUImageContext;
import com.gpuimage.GPUImageFilter;
import com.gpuimage.GPUImageFramebuffer;
import com.gpuimage.GPUImageUtils;
import com.gpuimage.GSize;
import com.gpuimage.sources.GPUImageOutput;
import com.gpuimage.GLog;

import java.nio.ByteBuffer;

/**
 * Created by j on 22/4/2018.
 */

public class GPUImageTwoInputFilter extends GPUImageFilter {
    public static final String kGPUImageTwoInputTextureVertexShaderString = "" +
            " attribute vec4 position;\n" +
            " attribute vec4 inputTextureCoordinate;\n" +
            " attribute vec4 inputTextureCoordinate2;\n" +
            " \n" +
            " varying vec2 textureCoordinate;\n" +
            " varying vec2 textureCoordinate2;\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     gl_Position = position;\n" +
            "     textureCoordinate = inputTextureCoordinate.xy;\n" +
            "     textureCoordinate2 = inputTextureCoordinate2.xy;\n" +
            " }";

    protected GPUImageFramebuffer mSecondInputFramebuffer;
    protected int mFilterSecondTextureCoordinateAttribute;
    protected int mFilterInputTextureUniform2;
    protected GPUImageContext.GPUImageRotationMode mInputRotation2;
    protected double mFirstFrameTime, mSecondFrameTime;

    protected boolean mHasSetFirstTexture, mHasReceivedFirstFrame, mHasReceivedSecondFrame;
    protected boolean mFirstFrameCheckDisabled, mSecondFrameCheckDisabled;

    protected ByteBuffer mSecondTextureCoordBuffer;

    public GPUImageTwoInputFilter(String fragmentShaderString) {
        this(kGPUImageTwoInputTextureVertexShaderString, fragmentShaderString);
    }

    public GPUImageTwoInputFilter(String vertexShaderString, String fragmentShaderString) {
        super(vertexShaderString, fragmentShaderString);

        mInputRotation2 = GPUImageContext.GPUImageRotationMode.kGPUImageNoRotation;

        mHasSetFirstTexture = false;

        mHasReceivedFirstFrame  = false;
        mHasReceivedSecondFrame = false;

        mFirstFrameCheckDisabled  = false;
        mSecondFrameCheckDisabled = false;

        mFirstFrameTime  = -1;
        mSecondFrameTime = -1;

        GDispatchQueue.runSynchronouslyOnVideoProcessingQueue(()->{
            GPUImageContext.useImageProcessingContext();

            mFilterSecondTextureCoordinateAttribute = mFilterProgram.attributeIndex("inputTextureCoordinate2");
            mFilterInputTextureUniform2 = mFilterProgram.uniformIndex("inputImageTexture2");

            GLES20.glEnableVertexAttribArray(mFilterSecondTextureCoordinateAttribute);
        });
    }

    public void disableFirstFrameCheck() {
        mFirstFrameCheckDisabled = true;
    }

    public void disableSecondFrameCheck() {
        mSecondFrameCheckDisabled = true;
    }

    @Override
    public void renderToTexture(float[] vertices, float[] textureCoordinates) {
        if (preventRendering) {
            removeFirstInputFramebuffer();
            removeSecondInputFramebuffer();
            return;
        }

        GPUImageContext.setActiveShaderProgram(mFilterProgram);
        mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebuffer(sizeOfFBO(), outputTextureOptions, false);
        mOutputFramebuffer.activateFramebuffer();

        if (mUsingNextFrameForImageCapture) {
            mOutputFramebuffer.lock();
        }

        GLES20.glClearColor(mBackgroundColorRed, mBackgroundColorGreen, mBackgroundColorBlue, mBackgroundColorAlpha);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFirstInputFramebuffer.texture());
        GLES20.glUniform1i(mFilterInputTextureUniform, 2);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mSecondInputFramebuffer.texture());
        GLES20.glUniform1i(mFilterInputTextureUniform2, 3);

        mVerticesCoordBuffer = GPUImageOutput.fillnativebuffer(mVerticesCoordBuffer, vertices);
        GLES20.glVertexAttribPointer(mFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, mVerticesCoordBuffer);
        mTextureCoordBuffer = GPUImageOutput.fillnativebuffer(mTextureCoordBuffer, textureCoordinates);
        GLES20.glVertexAttribPointer(mFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, mTextureCoordBuffer);
        mSecondTextureCoordBuffer = GPUImageOutput.fillnativebuffer(mSecondTextureCoordBuffer, textureCoordinatesForRotation(mInputRotation2));
        GLES20.glVertexAttribPointer(mFilterSecondTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, mSecondTextureCoordBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        removeFirstInputFramebuffer();
        removeSecondInputFramebuffer();

        if (mUsingNextFrameForImageCapture) {
            mImageCaptureSemaphore.release();
        }
    }

    @Override
    public int nextAvailableTextureIndex() {
        if (mHasSetFirstTexture) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public void setInputFramebuffer(GPUImageFramebuffer newInputFramebuffer, int textureIndex) {
        if (newInputFramebuffer != null) {
            if (textureIndex == 0) {
                removeFirstInputFramebuffer();
                mFirstInputFramebuffer = newInputFramebuffer;
                mHasSetFirstTexture = true;
                lockFirstInputFramebuffer();
            } else {
                removeSecondInputFramebuffer();
                mSecondInputFramebuffer = newInputFramebuffer;
                lockSecondInputFramebuffer();
            }
        }
    }

    @Override
    public void setInputSize(GSize newSize, int textureIndex) {
        if (textureIndex == 0) {
            super.setInputSize(newSize, textureIndex);
            if (newSize.equals(GSize.Zero)) {
                mHasSetFirstTexture = false;
            }
        }
    }

    @Override
    public void setInputRotation(GPUImageContext.GPUImageRotationMode newInputRotation, int textureIndex) {
        if (textureIndex == 0) {
            mInputRotation = newInputRotation;
        } else {
            mInputRotation2 = newInputRotation;
        }
    }

    @Override
    public GSize rotatedSize(GSize sizeToRotate, int textureIndex) {
        GSize rotatedSize = new GSize(sizeToRotate);

        GPUImageContext.GPUImageRotationMode rotationToCheck;
        if (textureIndex == 0) {
            rotationToCheck = mInputRotation;
        } else {
            rotationToCheck = mInputRotation2;
        }

        if (RotationSwapsWidthAndHeight(rotationToCheck)) {
            rotatedSize.width = sizeToRotate.height;
            rotatedSize.height = sizeToRotate.width;
        }

        return rotatedSize;
    }

    @Override
    public void newFrameReadyAtTime(double frameTime, int textureIndex) {
        if (mHasReceivedFirstFrame && mHasReceivedSecondFrame) {
            return;
        }

        boolean updatedMovieFrameOppositeStillImage = false;

        if (textureIndex == 0 && mFirstInputFramebuffer != null) {
            mHasReceivedFirstFrame = true;
            mFirstFrameTime = frameTime;
            if (mSecondFrameCheckDisabled) {
                mHasReceivedSecondFrame = true;
            }
            if (!GPUImageUtils.TimeIsInDefinite(frameTime)) {
                if (GPUImageUtils.TimeIsInDefinite(mSecondFrameTime)) {
                    updatedMovieFrameOppositeStillImage = true;
                }
            }
        } else if (mSecondInputFramebuffer != null){
            mHasReceivedSecondFrame = true;
            mSecondFrameTime = frameTime;
            if (mFirstFrameCheckDisabled) {
                mHasReceivedFirstFrame = true;
            }

            if (!GPUImageUtils.TimeIsInDefinite(frameTime)) {
                if (GPUImageUtils.TimeIsInDefinite(mFirstFrameTime)) {
                    updatedMovieFrameOppositeStillImage = true;
                }
            }
        }
        if ((mHasReceivedFirstFrame && mHasReceivedSecondFrame) || updatedMovieFrameOppositeStillImage) {
            double passOnFrameTime = (!GPUImageUtils.TimeIsInDefinite(mFirstFrameTime)) ? mFirstFrameTime : mSecondFrameTime;
            super.newFrameReadyAtTime(passOnFrameTime, 0);
            mHasReceivedFirstFrame  = false;
            mHasReceivedSecondFrame = false;
        }
    }

    protected void removeSecondInputFramebuffer() {
        if (mSecondInputFramebuffer != null) {
            mSecondInputFramebuffer.unlock();
        }
        mSecondInputFramebuffer = null;
    }

    protected void lockSecondInputFramebuffer() {
        if (mSecondInputFramebuffer != null) {
            mSecondInputFramebuffer.lock();
        }
    }
}

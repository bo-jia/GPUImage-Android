package com.gpuimage.outputs;

import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.view.Surface;

import com.gpuimage.GDispatchQueue;
import com.gpuimage.GLProgram;
import com.gpuimage.GLog;
import com.gpuimage.GPUImageContext;
import com.gpuimage.GPUImageFilter;
import com.gpuimage.GPUImageFramebuffer;
import com.gpuimage.GPUImageInput;
import com.gpuimage.GPUTextureOptions;
import com.gpuimage.GSize;
import com.gpuimage.mediautils.GMediaMovieWriter;
import com.gpuimage.sources.GPUImageOutput;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * Created by j on 16/12/2017.
 */

public class GPUImageMovieWriter implements GPUImageInput {

    private GMediaMovieWriter mWriter;
    private File mOutputFile;
    private EGLSurface mEGLSurface;
    private GPUImageContext mMovieWriterContext;
    private boolean mIsRecording;
    private double mPreviousFrameTime, mStartTime;

    private GLProgram mColorSwizzlingProgram;
    private GPUImageFramebuffer mFirstInputFramebuffer;

    protected GSize mVideoSize = new GSize();
    protected boolean mAlreadyFinishedRecording;
    protected ByteBuffer mTextureCoordBuffer, mVerticesCoordBuffer;
    protected GPUImageContext.GPUImageRotationMode mInputRotation;

    private int framebufferId, textureId;

    public Runnable completionRunnable = null;

    private int mColorSwizzlingPositionAttribute, mColorSwizzlingTextureCoordinateAttribute, mColorSwizzlingInputTextureUniform;

    public GPUImageMovieWriter(int width, int height, String path) {
        mVideoSize = new GSize(width, height);
        mIsRecording = false;
        mAlreadyFinishedRecording = false;
        mPreviousFrameTime = -1;
        mStartTime = -1;
        mOutputFile = new File(path);
        if (mOutputFile.exists()) mOutputFile.delete();

        mMovieWriterContext = new GPUImageContext();
        mMovieWriterContext.useSharedContext(GPUImageContext.sharedImageProcessingContext().context());

        mInputRotation = GPUImageContext.GPUImageRotationMode.kGPUImageNoRotation;
        GDispatchQueue.runSynchronouslyOnContextQueue(mMovieWriterContext, new Runnable() {
            @Override
            public void run() {

                mMovieWriterContext.useAsCurrentContext();
                mColorSwizzlingProgram = mMovieWriterContext.programForShaders(GPUImageFilter.kGPUImageVertexShaderString, GPUImageFilter.kGPUImagePassthroughFragmentShaderString);

                if (!mColorSwizzlingProgram.initialized) {
                    if (!mColorSwizzlingProgram.link()) {
                        String progLog = mColorSwizzlingProgram.programLog;
                        GLog.e("Program link log: " + progLog);
                        String fragLog = mColorSwizzlingProgram.fragmentShaderLog;
                        GLog.e("Fragment shader compile log: " + fragLog);
                        String vertLog = mColorSwizzlingProgram.vertexShaderLog;
                        GLog.e("Vertex shader compile log: " + vertLog);
                        mColorSwizzlingProgram = null;
                        GLog.a(false, "Filter shader link failed");
                    }
                }
                mMovieWriterContext.setContextShaderProgram(mColorSwizzlingProgram);
                mColorSwizzlingPositionAttribute = mColorSwizzlingProgram.attributeIndex("position");
                mColorSwizzlingTextureCoordinateAttribute = mColorSwizzlingProgram.attributeIndex("inputTextureCoordinate");
                mColorSwizzlingInputTextureUniform = mColorSwizzlingProgram.uniformIndex("inputImageTexture");

                GLES20.glEnableVertexAttribArray(mColorSwizzlingPositionAttribute);
                GLES20.glEnableVertexAttribArray(mColorSwizzlingTextureCoordinateAttribute);

                int[] tempID = new int[1];
                GLES20.glGenFramebuffers(1, tempID, 0);
                framebufferId = tempID[0];

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebufferId);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE1);

                GLES20.glGenTextures(1, tempID, 0);
                textureId = tempID[0];

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
                GPUTextureOptions mTextureOptions = new GPUTextureOptions();
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, mTextureOptions.magFilter);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, mTextureOptions.minFilter);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, mTextureOptions.wrapS);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, mTextureOptions.wrapT);

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, mTextureOptions.internalFormat, mVideoSize.width, mVideoSize.height, 0,
                        mTextureOptions.format, mTextureOptions.type, null);

                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textureId, 0);

                GLog.checkFramebufferStatus();

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

            }
        });
    }

    @Override
    public void setInputSize(GSize newSize, int textureIndex) {}

    private int count = 0;
    @Override
    public void newFrameReadyAtTime(final double frameTime, int texIndex) {
        if (!mIsRecording) {
            mFirstInputFramebuffer.unlock();
            return;
        }
        if (frameTime < 0 || Math.abs(frameTime - mPreviousFrameTime) < 1) {
            mFirstInputFramebuffer.unlock();
            return;
        }
        if (mStartTime < 0) {
            GDispatchQueue.runSynchronouslyOnContextQueue(mMovieWriterContext, new Runnable() {
                @Override
                public void run() {
                    if (mWriter == null) {
                        try {
                            mWriter = new GMediaMovieWriter(mVideoSize.width, mVideoSize.height, mOutputFile, null);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mEGLSurface = mMovieWriterContext.context().createWindowSurface(mWriter.getInputSurface());
                    }
                    mStartTime = frameTime;
                }
            });
        }

        final GPUImageFramebuffer inputFramebufferForRunnable = mFirstInputFramebuffer;
        GLES20.glFinish();

        mPreviousFrameTime = frameTime;
        GDispatchQueue.runAsynchronouslyOnContextQueue(mMovieWriterContext, new Runnable() {
            @Override
            public void run() {
                mMovieWriterContext.useAsCurrentContext(mEGLSurface);

                mWriter.drainEncoder(false);
                renderAtInternalSizeUsingFramebuffer(inputFramebufferForRunnable);
                mMovieWriterContext.context().setPresentationTime(mEGLSurface, (long) (frameTime * 1e6));
                mMovieWriterContext.swapBufferForDisply(mEGLSurface);
                inputFramebufferForRunnable.unlock();
            }
        });
    }

    @Override
    public void setInputFramebuffer(GPUImageFramebuffer newInputFramebuffer, int texIndex) {
        if (newInputFramebuffer != null) newInputFramebuffer.lock();
        mFirstInputFramebuffer = newInputFramebuffer;
    }

    @Override
    public void setInputRotation(GPUImageContext.GPUImageRotationMode newInputRotation, int textureIndex) {
        mInputRotation = newInputRotation;
    }

    @Override
    public GSize maximumOutputSize() {
        return mVideoSize;
    }

    @Override
    public void endProcessing() {
        if (completionRunnable != null) {
            if (!mAlreadyFinishedRecording) {
                mAlreadyFinishedRecording = true;
                completionRunnable.run();
            }
        }
    }

    @Override
    public boolean shouldIgnoreUpdatesToThisTarget() {
        return false;
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public boolean wantsMonochromeInput() {
        return false;
    }

    @Override
    public void setCurrentlyReceivingMonochromeInput(boolean newValue) {}

    @Override
    public int nextAvailableTextureIndex() {
        return 0;
    }

    private final static float squareVertices[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f,  1.0f,
            1.0f,  1.0f,
    };

    private void renderAtInternalSizeUsingFramebuffer(GPUImageFramebuffer inputFramebufferToUse) {
        mMovieWriterContext.useAsCurrentContext(mEGLSurface);

        mMovieWriterContext.setContextShaderProgram(mColorSwizzlingProgram);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glViewport(0, 0, mVideoSize.width, mVideoSize.height);

        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputFramebufferToUse.texture());
        GLES20.glUniform1i(mColorSwizzlingInputTextureUniform, 4);

        mVerticesCoordBuffer = GPUImageOutput.FillNativeBuffer(mVerticesCoordBuffer, squareVertices);
        GLES20.glVertexAttribPointer(mColorSwizzlingPositionAttribute, 2, GLES20.GL_FLOAT, false,0 , mVerticesCoordBuffer);
        mTextureCoordBuffer = GPUImageOutput.FillNativeBuffer(mTextureCoordBuffer, GPUImageFilter.textureCoordinatesForRotation(mInputRotation));
        GLES20.glVertexAttribPointer(mColorSwizzlingTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, mTextureCoordBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

    }

    public void startRecording() {
        mAlreadyFinishedRecording = false;
        mStartTime = -1;
        GDispatchQueue.runSynchronouslyOnContextQueue(mMovieWriterContext, new Runnable() {
            @Override
            public void run() {
                if (mWriter != null) return;
                try {
                    mWriter = new GMediaMovieWriter(mVideoSize.width, mVideoSize.height, mOutputFile, null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mEGLSurface = mMovieWriterContext.context().createWindowSurface(mWriter.getInputSurface());
            }
        });
        mIsRecording = true;
    }

    public void finishRecording() {
        finishRecordingWithCompletionHandler(null);
    }

    public void finishRecordingWithCompletionHandler(final Runnable handler) {
        GDispatchQueue.runSynchronouslyOnVideoProcessingQueue(new Runnable() {
            @Override
            public void run() {
                GDispatchQueue.runSynchronouslyOnContextQueue(mMovieWriterContext, new Runnable() {
                    @Override
                    public void run() {
                        if (mWriter != null) mWriter.drainEncoder(true);
                        cancelRecording();
                        if (handler != null) {
                            GDispatchQueue.runAsynchronouslyOnContextQueue(mMovieWriterContext, handler);
                        }
                    }
                });
            }
        });
    }

    public void cancelRecording() {
        mIsRecording = false;
        GDispatchQueue.runSynchronouslyOnContextQueue(mMovieWriterContext, new Runnable() {
            @Override
            public void run() {
                mAlreadyFinishedRecording = true;
                Surface surface = null;
                if (mWriter != null) {
                    surface = mWriter.getInputSurface();
                    mWriter.release();
                    mWriter = null;
                }
                if (mEGLSurface != null) {
                    mMovieWriterContext.context().releaseSurface(mEGLSurface);
                    mEGLSurface = null;
                }
                if (surface != null) {
                    surface.release();
                }
            }
        });
    }

    public double duration() {
        if (mStartTime < 0) return 0;
        if (mPreviousFrameTime > 0) return mPreviousFrameTime - mStartTime;
        return 0;
    }

    public GMediaMovieWriter getWriter() {
        return mWriter;
    }
}

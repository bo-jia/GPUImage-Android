package com.gpuimage.outputs;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.util.AttributeSet;
import android.view.TextureView;

import com.gpuimage.GDispatchQueue;
import com.gpuimage.GLProgram;
import com.gpuimage.GLog;
import com.gpuimage.GPUImageContext;
import com.gpuimage.GPUImageFilter;
import com.gpuimage.GPUImageFramebuffer;
import com.gpuimage.GPUImageInput;
import com.gpuimage.GSize;
import com.gpuimage.sources.GPUImageOutput;

import java.nio.ByteBuffer;

/**
 * Created by j on 5/12/2017.
 */

public class GPUImageView extends TextureView implements GPUImageInput, TextureView.SurfaceTextureListener {

    private EGLSurface mEGLSurface;
    private SurfaceTextureListener mSurfaceTextureListener;
    private GPUImageFramebuffer mRendererFramebuffer;

    public GPUImageView(Context context) {
        super(context);
    }

    public GPUImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setSurfaceTextureListener(this);
        commonInit();
    }

    @Override
    public void onSurfaceTextureAvailable(final SurfaceTexture surface, int width, int height) {
        GDispatchQueue.runSynchronouslyOnVideoProcessingQueue(new Runnable() {
            @Override
            public void run() {
                mEGLSurface = GPUImageContext.sharedImageProcessingContext().context().createWindowSurface(surface);
                GPUImageContext.sharedImageProcessingContext().useAsCurrentContext(mEGLSurface);
                presentFramebuffer();
            }
        });
        if (mSurfaceTextureListener != null)
            mSurfaceTextureListener.onSurfaceTextureAvailable(surface, width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (mSurfaceTextureListener != null)
            mSurfaceTextureListener.onSurfaceTextureSizeChanged(surface, width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mViewSize = new GSize(w, h);
        if (!mViewSize.equals(GSize.Zero)) {
            recalculateViewGeometry();
        }
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        GDispatchQueue.runAsynchronouslyOnVideoProcessingQueue(new Runnable() {
            @Override
            public void run() {
                GPUImageContext.sharedImageProcessingContext().context().releaseSurface(mEGLSurface);
            }
        });
        if (mSurfaceTextureListener != null)
            return mSurfaceTextureListener.onSurfaceTextureDestroyed(surface);
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (mSurfaceTextureListener != null)
            mSurfaceTextureListener.onSurfaceTextureUpdated(surface);
    }

    enum GPUImageFillModeType {
        kGPUImageFillModeStretch,                       // Stretch to fill the full view, which may distort the image outside of its normal aspect ratio
        kGPUImageFillModePreserveAspectRatio,           // Maintains the aspect ratio of the source image, adding bars of the specified background color
        kGPUImageFillModePreserveAspectRatioAndFill     // Maintains the aspect ratio of the source image, zooming in on its center to fill the view
    }

    public GPUImageFillModeType mFillMode;

    private float mBackgroundColorRed, mBackgroundColorGreen, mBackgroundColorBlue, mBackgroundColorAlpha;
    protected GSize mInputImageSize = new GSize();
    protected ByteBuffer mTextureCoordBuffer, mVerticesCoordBuffer;
    private GPUImageContext.GPUImageRotationMode mInputRotation;

    private GPUImageFramebuffer mInputFramebufferForDisplay;

    private GLProgram mDisplayProgram;

    private GSize mViewSize = new GSize();
    public boolean enabled;
    private float mImageVertices[] = new float[8];

    private int mDisplayPositionAttribute, mDisplayTextureCoordinateAttribute, mDisplayInputTextureUniform;

    private final static float noRotationTextureCoordinates[] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
    };

    private final static float rotateRightTextureCoordinates[] = {
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
    };

    private final static float rotateLeftTextureCoordinates[] = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };

    private final static float verticalFlipTextureCoordinates[] = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
    };

    private final static float horizontalFlipTextureCoordinates[] = {
            1.0f, 1.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f,
    };

    private final static float rotateRightVerticalFlipTextureCoordinates[] = {
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            0.0f, 1.0f,
    };

    private final static float rotateRightHorizontalFlipTextureCoordinates[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
    };

    private final static float rotate180TextureCoordinates[] = {
            1.0f, 0.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
    };

    private void commonInit() {
        mInputRotation = GPUImageContext.GPUImageRotationMode.kGPUImageNoRotation;
        GDispatchQueue.runSynchronouslyOnVideoProcessingQueue(new Runnable() {
            @Override
            public void run() {
                GPUImageContext.useImageProcessingContext();
                mDisplayProgram = GPUImageContext.sharedImageProcessingContext().programForShaders(GPUImageFilter.kGPUImageVertexShaderString, GPUImageFilter.kGPUImagePassthroughFragmentShaderString);
                if (!mDisplayProgram.initialized) {
                    if (!mDisplayProgram.link()) {
                        String progLog = mDisplayProgram.programLog;
                        GLog.e("Program link log: " + progLog);
                        String fragLog = mDisplayProgram.fragmentShaderLog;
                        GLog.e("Fragment shader compile log: " + fragLog);
                        String vertLog = mDisplayProgram.vertexShaderLog;
                        GLog.e("Vertex shader compile log: " + vertLog);
                        mDisplayProgram = null;
                        GLog.a(false, "Filter shader link failed");
                    }
                }
                GPUImageContext.setActiveShaderProgram(mDisplayProgram);
                mDisplayPositionAttribute = mDisplayProgram.attributeIndex("position");
                mDisplayTextureCoordinateAttribute = mDisplayProgram.attributeIndex("inputTextureCoordinate");
                mDisplayInputTextureUniform = mDisplayProgram.uniformIndex("inputImageTexture");

                GLES20.glEnableVertexAttribArray(mDisplayPositionAttribute);
                GLES20.glEnableVertexAttribArray(mDisplayTextureCoordinateAttribute);
                setBackgroundColor(0.0f, 0.0f, 0.0f, 1.0f);
                mFillMode = GPUImageFillModeType.kGPUImageFillModePreserveAspectRatio;
            }
        });
    }

    private void recalculateViewGeometry() {
        GDispatchQueue.runSynchronouslyOnVideoProcessingQueue(new Runnable() {
            @Override
            public void run() {
                if (mViewSize.equals(GSize.Zero) || mInputImageSize.equals(GSize.Zero)) {return;}
                float heightScaling = 1.f, widthScaling = 1.f;
                GSize currentViewSize = mViewSize;
                GSize insectSize = new GSize(mInputImageSize);
                if (currentViewSize.width < mInputImageSize.width || currentViewSize.height < mInputImageSize.height) {
                    if (mInputImageSize.width / mInputImageSize.height > currentViewSize.width / currentViewSize.height) {
                        insectSize.width = currentViewSize.width;
                        insectSize.height = currentViewSize.width * mInputImageSize.height / mInputImageSize.width;
                    } else {
                        insectSize.height = currentViewSize.height;
                        insectSize.width = currentViewSize.height * mInputImageSize.width / mInputImageSize.height;
                    }
                }

                switch (mFillMode) {
                    case kGPUImageFillModeStretch:
                    {
                        widthScaling = 1.f;
                        heightScaling = 1.f;
                    } break;
                    case kGPUImageFillModePreserveAspectRatio:
                    {
                        widthScaling = insectSize.width * 1.f / currentViewSize.width;
                        heightScaling = insectSize.height * 1.f / currentViewSize.height;
                    } break;
                    case kGPUImageFillModePreserveAspectRatioAndFill:
                    {
                        widthScaling = currentViewSize.height * 1.f / insectSize.height;
                        heightScaling = currentViewSize.width * 1.f / insectSize.width;
                    } break;
                }

                mImageVertices[0] = - widthScaling;
                mImageVertices[1] = - heightScaling;
                mImageVertices[2] = widthScaling;
                mImageVertices[3] = - heightScaling;
                mImageVertices[4] = - widthScaling;
                mImageVertices[5] = heightScaling;
                mImageVertices[6] = widthScaling;
                mImageVertices[7] = heightScaling;
            }
        });
    }

    @Override
    public void setInputSize(final GSize newSize, int textureIndex) {
        GDispatchQueue.runSynchronouslyOnVideoProcessingQueue(new Runnable() {
            @Override
            public void run() {
                GSize rotatedSize = new GSize(newSize);
                if (GPUImageFilter.RotationSwapsWidthAndHeight(mInputRotation)) {
                    rotatedSize.width = newSize.height;
                    rotatedSize.height = newSize.width;
                }
                if (!mInputImageSize.equals(rotatedSize)) {
                    mInputImageSize = rotatedSize;
                    recalculateViewGeometry();
                }
            }
        });
    }

    @Override
    public void newFrameReadyAtTime(double frameTime, int texIndex) {
        GDispatchQueue.runSynchronouslyOnVideoProcessingQueue(new Runnable() {
            @Override
            public void run() {

                GPUImageContext.useImageProcessingContext();

                if (mRendererFramebuffer == null || !mRendererFramebuffer.size().equals(mViewSize)) {
                    if (mRendererFramebuffer != null) mRendererFramebuffer.unlock();
                    mRendererFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebuffer(mViewSize, false);
                }

                if (mDisplayProgram == null) {
                    GLog.e("mDisplayProgram is null");
                }
                GPUImageContext.setActiveShaderProgram(mDisplayProgram);
                mRendererFramebuffer.activateFramebuffer();

                GLES20.glClearColor(mBackgroundColorRed, mBackgroundColorGreen, mBackgroundColorBlue, mBackgroundColorAlpha);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mInputFramebufferForDisplay.texture());
                GLES20.glUniform1i(mDisplayInputTextureUniform, 4);

                mVerticesCoordBuffer = GPUImageOutput.fillnativebuffer(mVerticesCoordBuffer, mImageVertices);
                GLES20.glVertexAttribPointer(mDisplayPositionAttribute, 2, GLES20.GL_FLOAT, false,0 , mVerticesCoordBuffer);
                mTextureCoordBuffer = GPUImageOutput.fillnativebuffer(mTextureCoordBuffer, textureCoordinatesForRotation(mInputRotation));
                GLES20.glVertexAttribPointer(mDisplayTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, mTextureCoordBuffer);

                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

                mInputFramebufferForDisplay.unlock();

                presentFramebuffer();
            }
        });
    }

    private void presentFramebuffer() {

        if (mRendererFramebuffer == null) return;

        GPUImageContext.useImageProcessingContext(mEGLSurface);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glViewport(0, 0, mViewSize.width, mViewSize.height);

        GLES20.glClearColor(mBackgroundColorRed, mBackgroundColorGreen, mBackgroundColorBlue, mBackgroundColorAlpha);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mRendererFramebuffer.texture());
        GLES20.glUniform1i(mDisplayInputTextureUniform, 4);

        mVerticesCoordBuffer = GPUImageOutput.fillnativebuffer(mVerticesCoordBuffer, GPUImageFilter.imageVertices);
        GLES20.glVertexAttribPointer(mDisplayPositionAttribute, 2, GLES20.GL_FLOAT, false,0 , mVerticesCoordBuffer);
        mTextureCoordBuffer = GPUImageOutput.fillnativebuffer(mTextureCoordBuffer, GPUImageFilter.textureCoordinatesForRotation(mInputRotation));
        GLES20.glVertexAttribPointer(mDisplayTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, mTextureCoordBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        GPUImageContext.sharedImageProcessingContext().swapBufferForDisply(mEGLSurface);
    }

    @Override
    public void setInputFramebuffer(GPUImageFramebuffer newInputFramebuffer, int texIndex) {
        mInputFramebufferForDisplay = newInputFramebuffer;
        if (mInputFramebufferForDisplay != null) mInputFramebufferForDisplay.lock();
    }

    @Override
    public void setInputRotation(GPUImageContext.GPUImageRotationMode newInputRotation, int textureIndex) {
        mInputRotation = newInputRotation;
    }

    @Override
    public GSize maximumOutputSize() {
        return mViewSize;
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

    public void setBackgroundColor(float redComponent, float greenComponent, float blueComponent, float alphaComponent) {
        mBackgroundColorRed = redComponent;
        mBackgroundColorGreen = greenComponent;
        mBackgroundColorBlue = blueComponent;
        mBackgroundColorAlpha = alphaComponent;
    }

    public void setFillMode(GPUImageFillModeType newValue) {
        mFillMode = newValue;
        recalculateViewGeometry();
    }

    public static float[] textureCoordinatesForRotation(GPUImageContext.GPUImageRotationMode rotationMode) {
        switch(rotationMode)
        {
            case kGPUImageNoRotation: return noRotationTextureCoordinates;
            case kGPUImageRotateLeft: return rotateLeftTextureCoordinates;
            case kGPUImageRotateRight: return rotateRightTextureCoordinates;
            case kGPUImageFlipVertical: return verticalFlipTextureCoordinates;
            case kGPUImageFlipHorizonal: return horizontalFlipTextureCoordinates;
            case kGPUImageRotateRightFlipVertical: return rotateRightVerticalFlipTextureCoordinates;
            case kGPUImageRotateRightFlipHorizontal: return rotateRightHorizontalFlipTextureCoordinates;
            case kGPUImageRotate180: return rotate180TextureCoordinates;
        }
        return noRotationTextureCoordinates;
    }

    @Override
    public void setSurfaceTextureListener(SurfaceTextureListener listener) {
        mSurfaceTextureListener = listener;
    }
}

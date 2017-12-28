package com.gpuimage;

import android.graphics.Bitmap;
import android.opengl.GLES20;

import com.gpuimage.sources.GPUImageOutput;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class GPUImageFilter extends GPUImageIO {

    public static final String kGPUImageVertexShaderString = "" +
        "attribute vec4 position;\n" +
        "attribute vec4 inputTextureCoordinate;\n" +
        " \n" +
        "varying vec2 textureCoordinate;\n" +
        " \n" +
        "void main()\n" +
        "{\n" +
        "    gl_Position = position;\n" +
        "    textureCoordinate = inputTextureCoordinate.xy;\n" +
        "}";

    public static final String kGPUImagePassthroughFragmentShaderString = "" +
        "varying highp vec2 textureCoordinate;\n" +
        " \n" +
            "uniform sampler2D inputImageTexture;\n" +
        " \n" +
        "void main()\n" +
        "{\n" +
        "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
        "}";

    public final static float noRotationTextureCoordinates[] = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
    };

    private final static float rotateLeftTextureCoordinates[] = {
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            0.0f, 1.0f,
    };

    private final static float rotateRightTextureCoordinates[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
    };

    private final static float verticalFlipTextureCoordinates[] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f,  0.0f,
            1.0f,  0.0f,
    };

    private final static float horizontalFlipTextureCoordinates[] = {
            1.0f, 0.0f,
            0.0f, 0.0f,
            1.0f,  1.0f,
            0.0f,  1.0f,
    };

    private final static float rotateRightVerticalFlipTextureCoordinates[] = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };

    private final static float rotateRightHorizontalFlipTextureCoordinates[] = {
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
    };

    private final static float rotate180TextureCoordinates[] = {
            1.0f, 1.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f,
    };

    public static final float imageVertices[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f,  1.0f,
            1.0f,  1.0f,
    };

    public boolean preventRendering = false;
    public boolean currentlyReceivingMonochromeInput = false;

    protected GPUImageContext.GPUImageRotationMode mInputRotation = GPUImageContext.GPUImageRotationMode.kGPUImageNoRotation;

    protected float mBackgroundColorRed = 0.f;
    protected float mBackgroundColorGreen = 0.f;
    protected float mBackgroundColorBlue = 0.f;
    protected float mBackgroundColorAlpha = 0.f;

    protected Semaphore mImageCaptureSemaphore = new Semaphore(0);
    protected GLProgram mFilterProgram;

    protected int mFilterPositionAttribute, mFilterTextureCoordinateAttribute;
    protected int mFilterInputTextureUniform;

    protected GPUImageFramebuffer mFirstInputFramebuffer;

    protected boolean mIsEndProcessing = false;

    public GPUImageFilter(final String vertexShaderString, final String fragmentShaderString) {
        mImageCaptureSemaphore.release();

        GDispatchQueue.runSynchronouslyOnVideoProcessingQueue(new Runnable() {
            @Override
            public void run() {
                GPUImageContext.useImageProcessingContext();
                mFilterProgram = GPUImageContext.sharedImageProcessingContext().programForShaders(vertexShaderString, fragmentShaderString);
                if (!mFilterProgram.initialized) {
                    if (!mFilterProgram.link()) {
                        String progLog = mFilterProgram.programLog;
                        GLog.e("Program link log: " + progLog);
                        String fragLog = mFilterProgram.fragmentShaderLog;
                        GLog.e("Fragment shader compile log: " + fragLog);
                        String vertLog = mFilterProgram.vertexShaderLog;
                        GLog.e("Vertex shader compile log: " + vertLog);
                        mFilterProgram = null;
                        GLog.a(false, "Filter shader link failed");
                    }
                }
                GPUImageContext.setActiveShaderProgram(mFilterProgram);
                mFilterPositionAttribute = mFilterProgram.attributeIndex("position");
                mFilterTextureCoordinateAttribute = mFilterProgram.attributeIndex("inputTextureCoordinate");
                mFilterInputTextureUniform = mFilterProgram.uniformIndex("inputImageTexture");
                GLES20.glEnableVertexAttribArray(mFilterPositionAttribute);
                GLES20.glEnableVertexAttribArray(mFilterTextureCoordinateAttribute);
            }
        });
    }

    public GPUImageFilter(String fragmentShaderString) {
        this(kGPUImageVertexShaderString, fragmentShaderString);
    }

    public GPUImageFilter() {
        this(kGPUImagePassthroughFragmentShaderString);
    }

    public void setupFilterForSize(GSize filterFrameSize) {}

    public void destroy() {

    }

    @Override
    public void useNextFrameForImageCapture() {
        mUsingNextFrameForImageCapture = true;

        if (!mImageCaptureSemaphore.tryAcquire()) return;
    }

    @Override
    public Bitmap newBitmapFromCurrentlyProcessedOutput() {
        double timeoutForImageCapture = 3.0;
        try {
            if (!mImageCaptureSemaphore.tryAcquire((long) timeoutForImageCapture, TimeUnit.SECONDS))
                return null;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }

        GPUImageFramebuffer framebuffer = framebufferForOutput();
        mUsingNextFrameForImageCapture = false;
        mImageCaptureSemaphore.release();

        Bitmap image = framebuffer.newBitmapFromFramebufferContents();

        return image;
    }
    public GSize sizeOfFBO() {
        GSize outputSize = maximumOutputSize();
        if (outputSize.equals(GSize.Zero) || mInputTextureSize.width < outputSize.width) {
            return mInputTextureSize;
        } else {
            return outputSize;
        }
    }

    public static float[] textureCoordinatesForRotation(GPUImageContext.GPUImageRotationMode rotationMode) {
        switch (rotationMode) {
            case kGPUImageNoRotation: return noRotationTextureCoordinates;
            case kGPUImageRotateLeft: return rotateLeftTextureCoordinates;
            case kGPUImageRotateRight: return rotateRightTextureCoordinates;
            case kGPUImageFlipVertical: return verticalFlipTextureCoordinates;
            case kGPUImageFlipHorizonal: return horizontalFlipTextureCoordinates;
            case kGPUImageRotateRightFlipVertical: return rotateRightVerticalFlipTextureCoordinates;
            case kGPUImageRotateRightFlipHorizontal: return rotateRightHorizontalFlipTextureCoordinates;
            case kGPUImageRotate180: return rotate180TextureCoordinates;
            default: return noRotationTextureCoordinates;
         }
    }

    public void renderToTexture(float vertices[], float textureCoordinates[]) {
        if (preventRendering) {
            mFirstInputFramebuffer.unlock();
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
        mVerticesCoordBuffer = GPUImageOutput.FillNativeBuffer(mVerticesCoordBuffer, vertices);
        GLES20.glVertexAttribPointer(mFilterPositionAttribute, 2, GLES20.GL_FLOAT, false, 0, mVerticesCoordBuffer);
        mTextureCoordBuffer = GPUImageOutput.FillNativeBuffer(mTextureCoordBuffer, textureCoordinates);
        GLES20.glVertexAttribPointer(mFilterTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, mTextureCoordBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        mFirstInputFramebuffer.unlock();

        if (mUsingNextFrameForImageCapture) {
            mImageCaptureSemaphore.release();
        }
    }

    public void informTargetsAboutNewFrameAtTime(double frameTime) {
        if (frameProcessingCompletionListener != null) {
            frameProcessingCompletionListener.frameProcessingCompletion(this, frameTime);
        }
        for (GPUImageInput currentTarget : mTargets) {
            if (currentTarget != targetToIgnoreForUpdates) {
                int indexOfObject = mTargets.indexOf(currentTarget);
                int textureIndex = mTargetTextureIndices.indexOf(indexOfObject);

                setInputFramebuffer(currentTarget, textureIndex);
                currentTarget.setInputSize(outputFrameSize(), textureIndex);
            }
        }

        framebufferForOutput().unlock();

        if (mUsingNextFrameForImageCapture) {
        } else {
            removeOutputFramebuffer();
        }

        for (GPUImageInput currentTarget : mTargets) {
            if (currentTarget != targetToIgnoreForUpdates) {
                int indexOfObject = mTargets.indexOf(currentTarget);
                int textureIndex = mTargetTextureIndices.get(indexOfObject);
                currentTarget.newFrameReadyAtTime(frameTime, textureIndex);
            }
        }
    }

    public GSize outputFrameSize() {
        return new GSize(mInputTextureSize);
    }

    public void setBackgroundColor(float redComponent, float greenComponent, float blueComponent, float alphaComponent) {
        mBackgroundColorRed = redComponent;
        mBackgroundColorGreen = greenComponent;
        mBackgroundColorBlue = blueComponent;
        mBackgroundColorAlpha = alphaComponent;
    }

    public void setInteger(int newInteger, String uniformName) {
        int uniformIndex = mFilterProgram.uniformIndex(uniformName);
        setInteger(newInteger, uniformIndex, mFilterProgram);
    }

    public void setInteger(final int intValue, final int uniform, final GLProgram shaderProgram) {

        GDispatchQueue.runAsynchronouslyOnVideoProcessingQueue(new Runnable() {
            @Override
            public void run() {
                GPUImageContext.setActiveShaderProgram(shaderProgram);
                GLES20.glUniform1i(uniform, intValue);
            }
        });
    }

    public void setFloat(final float floatValue, final int uniform, final GLProgram shaderProgram) {
        GDispatchQueue.runAsynchronouslyOnVideoProcessingQueue(new Runnable() {
            @Override
            public void run() {
                GPUImageContext.setActiveShaderProgram(shaderProgram);
                GLES20.glUniform1f(uniform, floatValue);
            }
        });
    }

    public void setFloat(float newFloat, String uniformName) {
        int uniformIndex = mFilterProgram.uniformIndex(uniformName);
        setFloat(newFloat, uniformIndex, mFilterProgram);
    }

    public void setSize(GSize newSize, String uniformName) {
        int uniformIndex = mFilterProgram.uniformIndex(uniformName);
        setSize(newSize, uniformIndex, mFilterProgram);
    }

    public void setSize(final GSize sizeValue, final int uniform, final GLProgram shaderProgram) {
        GDispatchQueue.runAsynchronouslyOnVideoProcessingQueue(new Runnable() {
            @Override
            public void run() {
                GPUImageContext.setActiveShaderProgram(shaderProgram);
                float sizeArray[] = new float[] {sizeValue.width, sizeValue.height};
                GLES20.glUniform2fv(uniform, 1, sizeArray, 0);
            }
        });
    }

    public void setFloatArray(float array[], String uniformName) {
        int uniformIndex = mFilterProgram.uniformIndex(uniformName);
        setFloatArray(array, uniformIndex, mFilterProgram);
    }

    public void setFloatArray(final float array[], final int uniform, final GLProgram shaderProgram) {
        GDispatchQueue.runAsynchronouslyOnVideoProcessingQueue(new Runnable() {
            @Override
            public void run() {
                GPUImageContext.setActiveShaderProgram(shaderProgram);
                GLES20.glUniform1fv(uniform, array.length, array, 0);
            }
        });
    }

    @Override
    public void setInputSize(GSize newSize, int textureIndex) {
        if (preventRendering) return;

        if (mOverrideInputSize) {
            if (mForcedMaximumSize.equals(GSize.Zero)) {
            } else {
                GSize tempSize = new GSize(newSize);
                if (newSize.width < mForcedMaximumSize.width || newSize.height < mForcedMaximumSize.height) {
                    if (newSize.width * mForcedMaximumSize.height > mForcedMaximumSize.width * newSize.height) {
                        tempSize.width = mForcedMaximumSize.width;
                        tempSize.height = tempSize.width * newSize.height / newSize.width;
                    } else {
                        tempSize.height = mForcedMaximumSize.height;
                        tempSize.width = tempSize.height * newSize.width / newSize.height;
                    }
                }
                mInputTextureSize = tempSize;
            }
        } else {
            GSize rotatedSize = rotatedSize(newSize, textureIndex);
            if (rotatedSize.equals(GSize.Zero)) {
                mInputTextureSize = rotatedSize;
            } else if (!mInputTextureSize.equals(rotatedSize)) {
                mInputTextureSize = rotatedSize;
            }
        }
        setupFilterForSize(sizeOfFBO());
    }

    @Override
    public void newFrameReadyAtTime(double frameTime, int texIndex) {
        renderToTexture(imageVertices, textureCoordinatesForRotation(mInputRotation));
        informTargetsAboutNewFrameAtTime(frameTime);
    }

    @Override
    public void setInputFramebuffer(GPUImageFramebuffer newInputFramebuffer, int texIndex) {
        mFirstInputFramebuffer = newInputFramebuffer;
        if (mFirstInputFramebuffer != null) mFirstInputFramebuffer.lock();
    }

    public static boolean RotationSwapsWidthAndHeight(GPUImageContext.GPUImageRotationMode inputRotation) {
        if (inputRotation == null) {
            GLog.e("inputRotation == null");
            return false;
        }

        switch (inputRotation) {
            case kGPUImageRotateLeft:
            case kGPUImageRotateRight:
            case kGPUImageRotateRightFlipVertical:
            case kGPUImageRotateRightFlipHorizontal:
                return true;
            default:
                return false;
        }
    }

    public GSize rotatedSize(GSize sizeToRotate, int textureIndex) {
        GSize rotatedSize = new GSize(sizeToRotate);
        if (RotationSwapsWidthAndHeight(mInputRotation)) {
            rotatedSize.width = sizeToRotate.height;
            rotatedSize.height = sizeToRotate.width;
        }
        return rotatedSize;
    }

    @Override
    public void setInputRotation(GPUImageContext.GPUImageRotationMode newInputRotation, int textureIndex) {
        mInputRotation = newInputRotation;
    }

    @Override
    public void forceProcessingAtSize(GSize frameSize) {
        if (frameSize.equals(GSize.Zero)) {
            mOverrideInputSize = false;
        } else {
            mOverrideInputSize = true;
            mInputTextureSize = new GSize(frameSize);
            mForcedMaximumSize = GSize.newZero();
        }
    }

    @Override
    public void forceProcessingAtSizeRespectingAspectRatio(GSize frameSize) {
        if (frameSize.equals(GSize.Zero)) {
            mOverrideInputSize = false;
            mInputTextureSize = GSize.newZero();
            mForcedMaximumSize = GSize.newZero();
        } else {
            mOverrideInputSize = true;
            mForcedMaximumSize = new GSize(frameSize);
        }
    }

    @Override
    public GSize maximumOutputSize() {
        return GSize.newZero();
    }

    @Override
    public void endProcessing() {
        if (!mIsEndProcessing) {
            mIsEndProcessing = true;
            for (GPUImageInput currentTarget : mTargets) {
                currentTarget.endProcessing();
            }
        }
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

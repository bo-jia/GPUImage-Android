package com.gpuimage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.gpuimage.cvutils.CVImageUtils;
import com.gpuimage.cvutils.CVPicture;
import com.gpuimage.sources.GPUImageOutput;

import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

/**
 * Created by j on 1/12/2017.
 */

public class GPUImagePicture extends GPUImageOutput {

    protected GSize mPixelSizeOfImage;
    protected boolean mHasProcessedImage;
    protected Semaphore mImageUpdateSemaphore;

    public GPUImagePicture(final Bitmap bitmap) {
        this(bitmap, false);
    }

    public GPUImagePicture(final Bitmap bitmap, boolean smoothlyScaleOutput) {
        shouldSmoothlyScaleOutput = smoothlyScaleOutput;
        mHasProcessedImage = false;
        mImageUpdateSemaphore = new Semaphore(0);
        mImageUpdateSemaphore.release();

        int widthOfImage  = bitmap.getWidth();
        int heightOfImage = bitmap.getHeight();

        GLog.a(widthOfImage > 0 && heightOfImage > 0, "Passed image must not be empty - it should be at least 1px tall and wide");

        final GSize finalPixelSizeToUseForTexture = computePixelSizeToUseForTexture(widthOfImage, heightOfImage);

        GDispatchQueue.runSynchronouslyOnVideoProcessingQueue(new Runnable() {
            @Override
            public void run() {
                GPUImageContext.useImageProcessingContext();

                mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebuffer(finalPixelSizeToUseForTexture, true);
                mOutputFramebuffer.disableReferenceCounting();

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOutputFramebuffer.texture());
                if (shouldSmoothlyScaleOutput) {
                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
                }

                Bitmap texBitmap = GPUImageUtils.getGLRGBABitmap(bitmap);

                if (texBitmap.getWidth() <= finalPixelSizeToUseForTexture.width && texBitmap.getHeight() <= finalPixelSizeToUseForTexture.height) {
                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, texBitmap, 0);
                } else {
                    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, finalPixelSizeToUseForTexture.width, finalPixelSizeToUseForTexture.height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
                    CVImageUtils.LoadBitmapForTexture(texBitmap, finalPixelSizeToUseForTexture.width, finalPixelSizeToUseForTexture.height);
                }

                if (shouldSmoothlyScaleOutput) {
                    GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
                }

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            }
        });
    }

    public GPUImagePicture(CVPicture picture, boolean smoothlyScaleOutput, boolean releasePicture) {
        shouldSmoothlyScaleOutput = smoothlyScaleOutput;
        mHasProcessedImage = false;
        mImageUpdateSemaphore = new Semaphore(0);
        mImageUpdateSemaphore.release();

        int widthOfImage = picture.getWidth();
        int heightOfImage = picture.getHeight();

        GLog.a(widthOfImage > 0 && heightOfImage > 0, "Passed image must not be empty - it should be at least 1px tall and wide");

        final GSize finalPixelSizeToUseForTexture = computePixelSizeToUseForTexture(widthOfImage, heightOfImage);

        GDispatchQueue.runSynchronouslyOnVideoProcessingQueue(new Runnable() {
            @Override
            public void run() {
                GPUImageContext.useImageProcessingContext();

                mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebuffer(finalPixelSizeToUseForTexture, true);
                mOutputFramebuffer.disableReferenceCounting();

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOutputFramebuffer.texture());
                if (shouldSmoothlyScaleOutput) {
                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
                }

                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, finalPixelSizeToUseForTexture.width, finalPixelSizeToUseForTexture.height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
                CVImageUtils.LoadCVPictureForTexture(picture.getHandler(), picture.getWidth(), picture.getHeight(), finalPixelSizeToUseForTexture.width, finalPixelSizeToUseForTexture.height);

                if (releasePicture) {
                    picture.release();
                }

                if (shouldSmoothlyScaleOutput) {
                    GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
                }

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            }
        });
    }

    private GSize computePixelSizeToUseForTexture(int width, int height) {
        mPixelSizeOfImage = new GSize(width, height);
        final GSize pixelSizeToUseForTexture = new GSize(mPixelSizeOfImage);

        GDispatchQueue.runSynchronouslyOnVideoProcessingQueue(()->{
            GSize scaledImageSizeToFitOnGPU = GPUImageContext.sizeThatFitsWithinATextureForSize(mPixelSizeOfImage);
            if (!scaledImageSizeToFitOnGPU.equals(mPixelSizeOfImage)) {
                mPixelSizeOfImage = new GSize(scaledImageSizeToFitOnGPU);
                pixelSizeToUseForTexture.width  = mPixelSizeOfImage.width;
                pixelSizeToUseForTexture.height = mPixelSizeOfImage.height;
            }
        });

        if (shouldSmoothlyScaleOutput) {
            float powerClosestToWidth = (float) Math.ceil(Math.log(mPixelSizeOfImage.width)/Math.log(2));
            float powerClosestToHeight = (float) Math.ceil(Math.log(mPixelSizeOfImage.height)/Math.log(2));

            pixelSizeToUseForTexture.width  = (int) Math.pow(2, powerClosestToWidth);
            pixelSizeToUseForTexture.height = (int) Math.pow(2, powerClosestToHeight);
        }

        return pixelSizeToUseForTexture;
    }

    public void release() {
        mOutputFramebuffer.enableReferenceCounting();
        mOutputFramebuffer.unlock();
    }

    @Override
    public void removeAllTargets() {
        super.removeAllTargets();
        mHasProcessedImage = false;
    }

    public void processImage() {
        processImage(null);
    }

    public boolean processImage(final Runnable completion) {
        mHasProcessedImage = true;
        if (!mImageUpdateSemaphore.tryAcquire()) {
            return false;
        }

        GDispatchQueue.runAsynchronouslyOnVideoProcessingQueue(new Runnable() {
            @Override
            public void run() {
                for (GPUImageInput currentTarget : mTargets) {
                    int indexOfObject = mTargets.indexOf(currentTarget);
                    int textureIndexOfTarget = mTargetTextureIndices.get(indexOfObject);

                    currentTarget.setCurrentlyReceivingMonochromeInput(false);
                    currentTarget.setInputSize(mPixelSizeOfImage, textureIndexOfTarget);
                    currentTarget.setInputFramebuffer(mOutputFramebuffer, textureIndexOfTarget);
                    currentTarget.newFrameReadyAtTime(0, textureIndexOfTarget);
                }

                mImageUpdateSemaphore.release();

                if (completion != null) {
                    completion.run();
                }
            }
        });

        return true;
    }

    public GSize outputImageSize() {
        return new GSize(mPixelSizeOfImage);
    }

    @Override
    public void addTarget(GPUImageInput newTarget, int textureLocation) {
        super.addTarget(newTarget, textureLocation);

        if (mHasProcessedImage) {
            newTarget.setInputSize(mPixelSizeOfImage, textureLocation);
            newTarget.newFrameReadyAtTime(0, textureLocation);
        }
    }
}

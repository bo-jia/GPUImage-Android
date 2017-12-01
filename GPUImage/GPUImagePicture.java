package GPUImage;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.util.concurrent.Semaphore;

/**
 * Created by j on 1/12/2017.
 */

public class GPUImagePicture extends GPUImageOutput {

    protected GSize mPixelSizeOfImage;
    protected boolean mHasProcessedImage;
    protected Semaphore mImageUpdateSemaphore;

    public GPUImagePicture(final Bitmap bitmap, boolean smoothlyScaleOutput) {
        mHasProcessedImage = false;
        mImageUpdateSemaphore = new Semaphore(0);
        mImageUpdateSemaphore.release();

        int widthOfImage = bitmap.getWidth();
        int heightOfImage = bitmap.getHeight();

        GLog.a(widthOfImage > 0 && heightOfImage > 0, "Passed image must not be empty - it should be at least 1px tall and wide");

        mPixelSizeOfImage = new GSize(widthOfImage, heightOfImage);
        GSize pixelSizeToUseForTexture = new GSize(mPixelSizeOfImage);

        GSize scaledImageSizeToFitOnGPU = GPUImageContext.sharedContext().sizeThatFitsWithinATextureForSize(mPixelSizeOfImage);
        if (!scaledImageSizeToFitOnGPU.equals(mPixelSizeOfImage)) {
            mPixelSizeOfImage = new GSize(scaledImageSizeToFitOnGPU);
            pixelSizeToUseForTexture = new GSize(mPixelSizeOfImage);
        }

        if (shouldSmoothlyScaleOutput) {
            float powerClosestToWidth = (float) Math.ceil(Math.log(mPixelSizeOfImage.width)/Math.log(2));
            float powerClosestToHeight = (float) Math.ceil(Math.log(mPixelSizeOfImage.height)/Math.log(2));

            pixelSizeToUseForTexture = new GSize((int) Math.pow(2, powerClosestToWidth), (int) Math.pow(2, powerClosestToHeight));
        }

        final GSize finalPixelSizeToUseForTexture = pixelSizeToUseForTexture;
        GPUImageProcessingQueue.sharedQueue().runSyn(new Runnable() {
            @Override
            public void run() {
                mOutputFramebuffer = GPUImageContext.sharedContext().framebufferCache.fetchFramebuffer(finalPixelSizeToUseForTexture, true);
                mOutputFramebuffer.disableReferenceCounting();

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOutputFramebuffer.texture());
                if (shouldSmoothlyScaleOutput) {
                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
                }

                try {
                    int type = GLUtils.getType(bitmap);
                    if (type != GLES20.GL_UNSIGNED_BYTE) {
                        Bitmap ubBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, ubBitmap, 0);
                    } else {
                        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap, 0);
                    }
                } catch (IllegalArgumentException e) {
                    Bitmap ubBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, ubBitmap, 0);
                }

                if (shouldSmoothlyScaleOutput) {
                    GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
                }

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            }
        });
    }

    public void destroy() {
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
        if (!mImageUpdateSemaphore.tryAcquire()) return false;

        GPUImageProcessingQueue.sharedQueue().runAsyn(new Runnable() {
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

                if (completion != null) completion.run();
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

package com.gpuimage;

import android.graphics.Bitmap;
import android.opengl.GLES20;

import java.nio.ByteBuffer;


public class GPUImageFramebuffer {

	private GSize mSize;
	private GPUTextureOptions mTextureOptions;
	private int mTexture;
	private boolean mMissingFramebuffer;

	private int mFramebuffer;

	private int mFramebufferReferenceCount;
	private boolean mReferenceCountingDisabled;

	public GPUImageFramebuffer(GSize framebufferSize) {
	    this(framebufferSize, null, false);
	}

	public GPUImageFramebuffer(GSize framebufferSize, GPUTextureOptions fboTextureOptions, boolean onlyGenerateTexture) {
		mTextureOptions = fboTextureOptions;
		mSize = framebufferSize;
		mFramebufferReferenceCount = 0;
		mReferenceCountingDisabled = false;
		mMissingFramebuffer = onlyGenerateTexture;

		if (mMissingFramebuffer) {

		} else {

		}
	}

	public GPUImageFramebuffer(GSize framebufferSize, int overriddenTexture) {

	}

	private void generateTexture() {

		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);

		int[] tempID = new int[1];
		GLES20.glGenTextures(1, tempID, 0);
		mTexture = tempID[0];

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);

		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, mTextureOptions.magFilter);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, mTextureOptions.minFilter);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, mTextureOptions.wrapS);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, mTextureOptions.wrapT);

	}

	private void generateFramebuffer() {
		GPUImageProcessingQueue.sharedQueue().runSyn(new Runnable() {
			@Override
			public void run() {
				int[] tempID = new int[1];
				GLES20.glGenFramebuffers(1, tempID, 0);
				mFramebuffer = tempID[0];

				GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);

				generateTexture();

				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);
				GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, mTextureOptions.internalFormat, mSize.width, mSize.height, 0,
						mTextureOptions.format, mTextureOptions.type, null);

				GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mTexture, 0);

				GLog.checkFramebufferStatus();

				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
			}
		});
	}

	public void destroyFramebuffer() {
		GPUImageProcessingQueue.sharedQueue().runSyn(new Runnable() {
			@Override
			public void run() {
				if (mFramebuffer != 0) {
					GLES20.glDeleteFramebuffers(1, new int[] {mFramebuffer}, 0);
					mFramebuffer = 0;
				}
				GLES20.glDeleteTextures(1, new int[] {mTexture}, 0);
			}
		});
	}

	public void activateFramebuffer() {
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);
		GLES20.glViewport(0, 0, mSize.width, mSize.height);
	}

	public void lock() {
		if (mReferenceCountingDisabled) return;
		mFramebufferReferenceCount++;
	}

	public void unlock() {
		if (mReferenceCountingDisabled) return;

		GLog.a(mFramebufferReferenceCount > 0, "Tried to overrelease a framebuffer, did you forget to call -useNextFrameForImageCapture before using -imageFromCurrentFramebuffer?");
		mFramebufferReferenceCount--;

		if (mFramebufferReferenceCount < 1) {
			GPUImageContext.sharedContext().framebufferCache.returnFrameBufferToCache(this);
		}
	}

	public void clearAllLocks() {
		mFramebufferReferenceCount = 0;
	}

	public void disableReferenceCounting() {
		mReferenceCountingDisabled = true;
	}

	public void enableReferenceCounting() {
		mReferenceCountingDisabled = false;
	}

	public int texture() {
		return mTexture;
	}

	public GSize size() {return new GSize(mSize); }

	public boolean missingFramebuffer() {return  mMissingFramebuffer;}

	public GPUTextureOptions textureOptions() {
		final  GPUTextureOptions textureOptions = mTextureOptions;
		return textureOptions;
	}

	public Bitmap newBitmapFromFramebufferContents() {
		final Bitmap bitmapFromBytes = Bitmap.createBitmap(mSize.width, mSize.height, Bitmap.Config.ARGB_8888);
		GPUImageProcessingQueue.sharedQueue().runSyn(new Runnable() {
			@Override
			public void run() {
				byte[] data = new byte[mSize.width * mSize.height * 4];
				ByteBuffer buffer = ByteBuffer.wrap(data);
				GLES20.glReadPixels(0, 0, mSize.width, mSize.height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
				bitmapFromBytes.copyPixelsFromBuffer(buffer);
				unlock();
			}
		});
		return bitmapFromBytes;
	}
}

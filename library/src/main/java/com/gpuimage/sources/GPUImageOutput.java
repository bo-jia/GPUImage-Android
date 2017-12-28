package com.gpuimage.sources;

import android.graphics.Bitmap;
import android.os.Build;

import com.gpuimage.GDispatchQueue;
import com.gpuimage.GPUImageContext;
import com.gpuimage.GPUImageFramebuffer;
import com.gpuimage.GPUImageInput;
import com.gpuimage.GPUTextureOptions;
import com.gpuimage.GSize;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

public class GPUImageOutput {
	public interface FrameProcessingCompletionListener { void frameProcessingCompletion(GPUImageOutput output, double frameTime); }

	protected GSize mInputTextureSize = new GSize();
	protected GSize mCachedMaximumOutputSize = new GSize();
	protected GSize mForcedMaximumSize = new GSize();

	protected GPUImageFramebuffer mOutputFramebuffer = null;
	protected boolean mOverrideInputSize;
	protected Vector<GPUImageInput> mTargets = new Vector<>();
	protected Vector<Integer> mTargetTextureIndices = new Vector<>();
	protected GSize cachedMaximumOutputSize = null;

	protected boolean mUsingNextFrameForImageCapture = false;
	protected boolean mAllTargetsWantMonochromeData = true;

	protected ByteBuffer mTextureCoordBuffer, mVerticesCoordBuffer;

	public GPUTextureOptions outputTextureOptions = new GPUTextureOptions();
	public boolean enable = true;

	public GPUImageInput targetToIgnoreForUpdates = null;
	public FrameProcessingCompletionListener frameProcessingCompletionListener = null;

	public boolean shouldSmoothlyScaleOutput = false;


	public void setInputFramebuffer(GPUImageInput target, int inputTextureIndex) {
		target.setInputFramebuffer(framebufferForOutput(), inputTextureIndex);
	}

	public GPUImageFramebuffer framebufferForOutput() {
		return mOutputFramebuffer;
	}

	public void removeOutputFramebuffer() {
		mOutputFramebuffer = null;
	}


	public void notifyTargetsAboutNewOutputTexture() {
		for (GPUImageInput currentTarget : mTargets) {
			int indexOfObject = mTargets.indexOf(currentTarget);
			int textureIndex = mTargetTextureIndices.indexOf(indexOfObject);

			setInputFramebuffer(currentTarget, textureIndex);
		}
	}

	public Vector<GPUImageInput> targets() {
		return new Vector<>(mTargets);
	}

	public void addTarget(GPUImageInput newTarget) {
		int nextAvailableTextureIndex = newTarget.nextAvailableTextureIndex();
		addTarget(newTarget, nextAvailableTextureIndex);

		if (newTarget.shouldIgnoreUpdatesToThisTarget()) {
			targetToIgnoreForUpdates = newTarget;
		}
	}
	
	public void addTarget(final GPUImageInput newTarget, final int textureLocation) {
		if (mTargets.contains(newTarget)) return;

		cachedMaximumOutputSize = GSize.newZero();

		GDispatchQueue.runSynchronouslyOnVideoProcessingQueue(new Runnable() {
			@Override
			public void run() {
				setInputFramebuffer(newTarget, textureLocation);
				mTargets.add(newTarget);
				mTargetTextureIndices.add(textureLocation);

				mAllTargetsWantMonochromeData = (mAllTargetsWantMonochromeData && newTarget.wantsMonochromeInput());
			}
		});
	}
	
	public void removeTarget(final GPUImageInput targetToRemove) {
		if (!mTargets.contains(targetToRemove)) return;

		if (targetToIgnoreForUpdates == targetToRemove) targetToIgnoreForUpdates = null;

		cachedMaximumOutputSize = GSize.newZero();

		final int indexOfObject = mTargets.indexOf(targetToRemove);
		final int textureIndexOfTarget = mTargetTextureIndices.indexOf(targetToRemove);

		GDispatchQueue.runSynchronouslyOnVideoProcessingQueue(new Runnable() {
			@Override
			public void run() {
				targetToRemove.setInputSize(GSize.newZero(), textureIndexOfTarget);
				targetToRemove.setInputRotation(GPUImageContext.GPUImageRotationMode.kGPUImageNoRotation, textureIndexOfTarget);

				mTargetTextureIndices.removeElementAt(indexOfObject);
				mTargets.remove(targetToRemove);
				targetToRemove.endProcessing();
			}
		});
	}
	
	public void removeAllTargets() {
		cachedMaximumOutputSize = GSize.newZero();
		GDispatchQueue.runSynchronouslyOnVideoProcessingQueue(new Runnable() {
			@Override
			public void run() {
				for (GPUImageInput targetToRemove : mTargets) {
					int indexOfObject = mTargets.indexOf(targetToRemove);
					int textureIndexOfTarget = mTargetTextureIndices.get(indexOfObject);

					targetToRemove.setInputSize(GSize.newZero(), textureIndexOfTarget);
					targetToRemove.setInputRotation(GPUImageContext.GPUImageRotationMode.kGPUImageNoRotation, textureIndexOfTarget);
				}

				mTargets.removeAllElements();
				mTargetTextureIndices.removeAllElements();

				mAllTargetsWantMonochromeData = true;
			}
		});
	}

	public void forceProcessingAtSize(GSize frameSize) {}

	public void forceProcessingAtSizeRespectingAspectRatio(GSize frameSize) {}

	public void useNextFrameForImageCapture() {}

	public Bitmap newBitmapFromCurrentlyProcessedOutput() {return null;}

	// TODO: 30/11/2017 还没有实现需要GPUImagePicture
	public Bitmap newBitmapByFilteringBitmap(Bitmap imageToFilter) {
		return null;
	}

	public boolean providesMonochromeOutput() {return false;}

	// TODO: 30/11/2017 还没有实现audio encoding 相关
	public void setAudioEncodingTarget() {}

	public static ByteBuffer FillNativeBuffer(ByteBuffer buffer, float[] values) {
		if (values == null) {
            return buffer;
        }
		if (buffer == null || buffer.order() != ByteOrder.nativeOrder() || buffer.capacity() != values.length * floatSize) {
            buffer = ByteBuffer.allocateDirect(values.length * Float.SIZE / Byte.SIZE).order(ByteOrder.nativeOrder());
        }
		buffer.asFloatBuffer().put(values);
		return buffer;
	}
}

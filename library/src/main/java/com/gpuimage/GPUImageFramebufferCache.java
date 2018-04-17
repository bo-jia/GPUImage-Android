package com.gpuimage;

import android.annotation.SuppressLint;

import java.util.HashMap;
import java.util.Map.Entry;

@SuppressLint("DefaultLocale")
public class GPUImageFramebufferCache {

	/**
	* If the size of framebuffers is greater than max cache size, we will free some old framebuffers
	* */
	private long mMaxCacheSize = 512 * 1024 * 1024;

	private long mMemSize = 0;

    private HashMap<String, GPUImageFramebuffer> mFramebufferCache = new HashMap<>();
    private HashMap<String, Integer> mFramebufferTypeCounts = new HashMap<>();
	private HashMap<String, Long> mFramebufferTimestamp = new HashMap<>();

    public String hash(GSize size, GPUTextureOptions textureOptions, boolean onlyTexture) {
		if (onlyTexture) {
			return String.format("%dx%d-%d:%d:%d:%d:%d:%d:%d-NOFB", size.width, size.height,
					textureOptions.minFilter, textureOptions.magFilter, textureOptions.wrapS, textureOptions.wrapT,
					textureOptions.internalFormat, textureOptions.format, textureOptions.type);
		} else {
			return String.format("%dx%d-%d:%d:%d:%d:%d:%d:%d", size.width, size.height,
					textureOptions.minFilter, textureOptions.magFilter, textureOptions.wrapS, textureOptions.wrapT,
					textureOptions.internalFormat, textureOptions.format, textureOptions.type);
		}
	}

	public GPUImageFramebuffer fetchFramebuffer(final GSize framebufferSize, final GPUTextureOptions textureOptions, final boolean onlyTexture) {
		final GPUImageFramebuffer[] framebufferFromCache = {null};
    	GDispatchQueue.runSynchronouslyOnVideoProcessingQueue(new Runnable() {
			@Override
			public void run() {
				String lookupHash = hash(framebufferSize, textureOptions, onlyTexture);
				Integer numberOfMatchingTexturesInCache = mFramebufferTypeCounts.get(lookupHash);
				int numberOfMatchingTextures = (numberOfMatchingTexturesInCache == null ? 0 : numberOfMatchingTexturesInCache);

				if (numberOfMatchingTextures < 1) {
					framebufferFromCache[0] = newFramebuffer(framebufferSize, textureOptions, onlyTexture);
				} else {
					int currentTextureID = (numberOfMatchingTextures - 1);
					while ((framebufferFromCache[0] == null) && (currentTextureID >= 0))
					{
						String textureHash = lookupHash + "-" + currentTextureID;
						framebufferFromCache[0] = mFramebufferCache.get(textureHash);
						// Test the values in the cache first, to see if they got invalidated behind our back
						if (framebufferFromCache[0] != null)
						{
							// Withdraw this from the cache while it's in use
							mMemSize -= framebufferFromCache[0].size().width * framebufferFromCache[0].size().height * 4;
                    		mFramebufferCache.remove(textureHash);
                    		mFramebufferTimestamp.remove(textureHash);
						}
						currentTextureID--;
					}
					currentTextureID++;
					mFramebufferTypeCounts.put(lookupHash, currentTextureID);

					if (framebufferFromCache[0] == null) {
						framebufferFromCache[0] = newFramebuffer(framebufferSize, textureOptions, onlyTexture);
					}
				}
			}
		});
		framebufferFromCache[0].lock();
		return framebufferFromCache[0];
	}

	public GPUImageFramebuffer fetchFramebuffer(final GSize framebufferSize, final boolean onlyTexture) {
		GPUTextureOptions defaultTextureOptions = new GPUTextureOptions();
		return fetchFramebuffer(framebufferSize, defaultTextureOptions, onlyTexture);
	}

    public void returnFrameBufferToCache(final GPUImageFramebuffer framebuffer) {
    	framebuffer.clearAllLocks();
    	GDispatchQueue.runAsynchronouslyOnVideoProcessingQueue(new Runnable() {
			@Override
			public void run() {
				GSize framebufferSize = framebuffer.size();
				GPUTextureOptions framebufferTextureOptions = framebuffer.textureOptions();

				String lookupHash = hash(framebufferSize, framebufferTextureOptions, framebuffer.missingFramebuffer());
				Integer numberOfMatchingTexturesInCache = mFramebufferTypeCounts.get(lookupHash);
				int numberOfMatchingTextures = (numberOfMatchingTexturesInCache == null ? 0 : numberOfMatchingTexturesInCache);

				String textureHash = lookupHash + "-" + numberOfMatchingTextures;

				mMemSize += framebufferSize.width * framebufferSize.height * 4;
				mFramebufferCache.put(textureHash, framebuffer);
				mFramebufferTypeCounts.put(lookupHash, numberOfMatchingTextures + 1);
				mFramebufferTimestamp.put(textureHash, System.currentTimeMillis());
			}
		});
    }

    private GPUImageFramebuffer newFramebuffer(GSize framebufferSize, GPUTextureOptions textureOptions, boolean onlyTexture) {
		long frameSize = framebufferSize.width * framebufferSize.height * 4;

    	while (mMemSize + frameSize > mMaxCacheSize && frameSize < mMaxCacheSize) {

			// find an oldest framebuffer
    		long oldestTimestamp = Long.MAX_VALUE;
    		String oldestTextureHash = null;
			for (Entry<String, Long> entry : mFramebufferTimestamp.entrySet()) {
				long timestamp = entry.getValue();
				if (timestamp < oldestTimestamp) {
					oldestTextureHash = entry.getKey();
					oldestTimestamp = timestamp;
				}
			}

			// get lookupHash of the oldest framebuffer
			GPUImageFramebuffer specialSizeFramebuffer = mFramebufferCache.get(oldestTextureHash);
			String lookupHash = hash(specialSizeFramebuffer.size(), specialSizeFramebuffer.textureOptions(), specialSizeFramebuffer.missingFramebuffer());

			// get a texture hash as the same size of the oldest framebuffer
			Integer numberOfMatchingTexturesInCache = mFramebufferTypeCounts.get(lookupHash);
			int numberOfMatchingTextures = (numberOfMatchingTexturesInCache == null ? 0 : numberOfMatchingTexturesInCache);

			if (numberOfMatchingTextures > 0) {
				int currentTextureID = (numberOfMatchingTextures - 1);
				String textureHash = lookupHash + "-" + currentTextureID;
				GPUImageFramebuffer framebuffer = mFramebufferCache.get(textureHash);
				if (framebuffer != null) {
					mMemSize -= framebuffer.size().width * framebuffer.size().height * 4;
					mFramebufferCache.remove(textureHash);
					Long timestamp = mFramebufferTimestamp.remove(textureHash);

					// swap the timestamp
					if (!oldestTextureHash.equals(textureHash)) {
						mFramebufferTimestamp.put(oldestTextureHash, timestamp);
					}

					GLog.i("free a framebuffer: " + textureHash);
					GLog.i("current cache size: " + mMemSize);
					framebuffer.destroyFramebuffer();
				}

				mFramebufferTypeCounts.put(lookupHash, currentTextureID);
			}
		}

    	return new GPUImageFramebuffer(framebufferSize, textureOptions, onlyTexture);
	}

    public void purgeAllUnassignedFrameBuffers() {
    	GDispatchQueue.runAsynchronouslyOnVideoProcessingQueue(new Runnable() {
			@Override
			public void run() {
				for (Entry<String, GPUImageFramebuffer> entry : mFramebufferCache.entrySet()) {
					entry.getValue().destroyFramebuffer();
				}
				mFramebufferCache.clear();
				mFramebufferTypeCounts.clear();
				mFramebufferTimestamp.clear();
				mMemSize = 0;
			}
		});
    }
}

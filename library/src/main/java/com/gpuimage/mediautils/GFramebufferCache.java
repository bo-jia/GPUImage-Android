package com.gpuimage.mediautils;

import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;
import com.gpuimage.GLog;

/**
 * Created by Felix on 15/4/2018.
 *
 * This provides a cache in memory for GMediaPlayer.
 * If the size of frames is greater than the max size, we will random drop a buffered frame.
 */

public class GFramebufferCache {
    private long mMaxCacheSize = 16 * 1000 * 1000;

    private LinkedList<GFramebuffer> mProducer = new LinkedList<>();
    private LinkedList<GFramebuffer> mConsumer = new LinkedList<>();
    private long mCacheSize = 0;
    private Random randDrop = new Random();

    public byte[] buffer;
    public int width, height;
    public double timestamp;

    public synchronized void pushFramebuffer(byte[] data, int width, int height, double timestamp) {
        assert (data != null && data.length > 0);
        GFramebuffer framebuffer = null;

        if (mProducer.size() > 0) {
            framebuffer = mProducer.poll();
        } else if (mCacheSize + data.length < mMaxCacheSize) {
            framebuffer = new GFramebuffer(data.length);
            mCacheSize += data.length;
        }

        if (framebuffer == null && mConsumer.size() > 0) {
            int idx = randDrop.nextInt(mConsumer.size());
            framebuffer = mConsumer.remove(idx);
        }
        if (framebuffer != null) {
            framebuffer.setData(data, width, height, timestamp);
            mConsumer.add(framebuffer);
        }
    }

    public synchronized boolean popFramebuffer() {
        GFramebuffer framebuffer = null;

        if (mConsumer.size() > 0) {
            framebuffer = mConsumer.poll();
            if (buffer == null || buffer.length != framebuffer.length) {
                buffer = new byte[framebuffer.length];
            }

            framebuffer.getData(buffer);
            width     = framebuffer.width;
            height    = framebuffer.height;
            timestamp = framebuffer.timestamp;
        }

        if (framebuffer != null) {
            mProducer.add(framebuffer);
            return true;
        }
        return false;
    }

    public synchronized void release() {
        for (GFramebuffer framebuffer : mProducer) {
            framebuffer.release();
        }
        for (GFramebuffer framebuffer : mConsumer) {
            framebuffer.release();
        }
        buffer = null;
        width  = 0;
        height = 0;
        timestamp = 0;
        mCacheSize = 0;
    }
}

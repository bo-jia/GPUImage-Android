package com.gpuimage.mediautils;

import com.gpuimage.GDispatchQueue;
import com.gpuimage.GLog;
import com.gpuimage.sources.GPUImageMovie;

import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by j on 14/12/2017.
 */


public class GMediaPlayer {

    private final String TAG = "GMediaPlayer";

    public interface FrameReady {
        void newFrame(byte yuv[], int width, int height, double timestamp);
    }

    private GMediaVideoReader mVideoReader = new GMediaVideoReader();;

    private LinkedList<Runnable> mQueue = new LinkedList<>();
    private Thread mPlayerThread;

    private ReentrantLock mPlayingLock = new ReentrantLock();
    private boolean mPlaying = false;
    private double mVideoStartTime  = 0;
    private double mSystemStartTime = 0;
    private boolean mDropable = true;
    private FrameReady mFrameReady;

    private Semaphore mPauseSem = new Semaphore(0);

    public GMediaPlayer() {
        mPlayerThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    LinkedList<Runnable> tempQueue = new LinkedList<>();
                    synchronized (mQueue) {
                        try {
                            mQueue.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        tempQueue.addAll(mQueue);
                        mQueue.clear();
                    }

                    while (!tempQueue.isEmpty()) {
                        tempQueue.removeFirst().run();
                    }
                }
            }
        };
        mPlayerThread.start();
    }

    public void loadMP4(String path) {
        mVideoReader.loadMP4(path);
    }

    public void play() {
        if (mPlaying) return;
        mSystemStartTime = System.currentTimeMillis();
        synchronized (mQueue) {
            mQueue.add(new Runnable() {
                @Override
                public void run() {
                    mPlayingLock.lock();
                    mPlaying = true;
                    mPlayingLock.unlock();
                    while (mPlaying) {
                        boolean ret = mVideoReader.readFrame();
                        if (ret) {
                            double timestamp = mVideoReader.getTimestamp();
                            double diffVideo = (timestamp - mVideoStartTime);
                            double diffSystem = System.currentTimeMillis() - mSystemStartTime;
                            if (diffVideo > diffSystem + 1) {
                                try {
                                    Thread.sleep((long) (diffVideo - diffSystem));
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (diffVideo < diffSystem - 1 && mDropable) {
                            } else {
                                if (mFrameReady != null) mFrameReady.newFrame(mVideoReader.getNV12Data(), mVideoReader.getFrameWidth(), mVideoReader.getFrameHeight(), mVideoReader.getTimestamp());
                            }
                        } else {
                            mPlaying = false;
                        }
                    }
                    GLog.i("end of play");
                }
            });
            mQueue.notifyAll();
        }
    }

    public void pause() {
        try {
            mPlayingLock.lock();
            if (mPlaying) {
                mPlaying = false;
                mPauseSem.acquire();
            }
            mPlayingLock.unlock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        pause();
        mVideoStartTime = 0;
        mVideoReader.stop();
    }

    public void start() {
        mVideoReader.start();
    }

    public void setFrameReadyListener(FrameReady frameReady) {
        mFrameReady = frameReady;
    }

    public static void defaultConfig(GMediaPlayer player, final GPUImageMovie movie) {
        player.setFrameReadyListener(new FrameReady() {
            @Override
            public void newFrame(byte[] yuv, final int width, final int height, final double timestamp) {
                final byte[] buffer = new byte[yuv.length];
                System.arraycopy(yuv, 0, buffer, 0, buffer.length);
                final double t0 = System.currentTimeMillis();
                GDispatchQueue.runAsynchronouslyOnVideoProcessingQueue(new Runnable() {
                    @Override
                    public void run() {
                        if (System.currentTimeMillis() - t0 > 50) return;
                        movie.processMovieFrame(buffer, width, height, timestamp);
                    }
                });
            }
        });
    }
}

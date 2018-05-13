package com.gpuimage.mediautils;

import android.media.MediaPlayer;

import com.gpuimage.GLog;
import com.gpuimage.GSize;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by j on 14/12/2017.
 */


public class GMediaPlayer {

    private final String TAG = "GMediaPlayer";

    public interface PlayerCallback {
        void onNewFrame(byte nv12[], int width, int height, double timestamp);
        void onEnd();
    }


    private GMediaMovieReader mVideoReader = new GMediaMovieReader();
    private GMediaVideoDecoder mVideoDecoder;

    private MediaPlayer mAudioPlayer = new MediaPlayer();

    private LinkedList<Runnable> mQueue = new LinkedList<>();
    private Thread mPlayerThread;

    private ReentrantLock mPlayingLock = new ReentrantLock();
    private boolean mPlaying = false;
    private double mVideoStartTime  = 0;
    private double mSystemStartTime = 0;
    private boolean mDropable = true;
    private Vector<PlayerCallback> mPlayerCallbackQueue = new Vector<>();
    private boolean mActive = true;
    private Semaphore mPauseSem = new Semaphore(0);

    private boolean mLooping = false;

    public GMediaPlayer() {
        mPlayerThread = new Thread() {
            @Override
            public void run() {
                while (mActive) {
                    Runnable task = null;
                    synchronized (mQueue) {
                        if (!mQueue.isEmpty()) {
                            task = mQueue.removeFirst();
                        }
                    }
                    if (task != null) {
                        task.run();
                    } else {
                        synchronized (mQueue) {
                            try {
                                mQueue.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        };
        mPlayerThread.start();
    }

    public void loadMP4(String path) {
        mVideoReader.loadMP4(path, GMediaMovieReader.Type.Video);
        try {
            mVideoDecoder = new GMediaVideoDecoder(mVideoReader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mAudioPlayer.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void play() {
        if (mPlaying) {
            return;
        }
        synchronized (mQueue) {
            mQueue.add(new Runnable() {
                @Override
                public void run() {
                    mPlayingLock.lock();
                    boolean pauseEnd = true;
                    mPlaying = true;
                    mPlayingLock.unlock();
                    mVideoStartTime = -1;
                    while (mPlaying && mActive) {
                        boolean ret = mVideoDecoder.decodeNextFrame();
                        if (ret) {
                            double timestamp = mVideoDecoder.timestampMs();
                            if (mVideoStartTime < 0) {
                                mAudioPlayer.seekTo((int) timestamp);
                                mAudioPlayer.start();
                                mVideoStartTime  = timestamp;
                                mSystemStartTime = System.currentTimeMillis();
                            }
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
                                for (PlayerCallback callback : mPlayerCallbackQueue) {
                                    GSize frameSize = mVideoDecoder.getSize();
                                    callback.onNewFrame(mVideoDecoder.getFrameNV12Data(), frameSize.width, frameSize.height, mVideoDecoder.timestampMs());
                                }
                            }
                        } else {
                            mVideoDecoder.stop();
                            mVideoDecoder.start();
                            if (mLooping) {
                                for (PlayerCallback callback: mPlayerCallbackQueue) {
                                    callback.onEnd();
                                }
                                mVideoStartTime = -1;
                                GLog.i("loop movie");
                            } else {
                                mPlayingLock.lock();
                                if (mPlaying) {
                                    pauseEnd = false;
                                    mPlaying = false;
                                }
                                mPlayingLock.unlock();
                            }
                        }
                    }
                    mAudioPlayer.pause();

                    if (pauseEnd) {
                        GLog.i("pause");
                        mPauseSem.release();
                    } else {
                        for (PlayerCallback callback: mPlayerCallbackQueue) {
                            callback.onEnd();
                        }
                        GLog.i("end of play");
                        synchronized (mQueue) {
                            mQueue.notifyAll();
                        }
                    }
                }
            });
            mQueue.notifyAll();
        }
    }

    public void setLooping(boolean looping) {
        mLooping = looping;
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
        mVideoDecoder.stop();
        mAudioPlayer.stop();
    }

    /**
     * call stop() first, then release the player
     */
    public void release() {
        mActive = false;
        synchronized (mQueue) {
            mQueue.notifyAll();
        }
        mVideoDecoder.release();
        mAudioPlayer.release();
    }

    public void start() {
        mVideoDecoder.start();
        try {
            mAudioPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reset() {

    }

    public void addPlayerListener(PlayerCallback callback) {
        if (callback != null) {
            mPlayerCallbackQueue.add(callback);
        }
    }

    public void removePlayerListener(PlayerCallback callback) {
        if (callback != null) {
            mPlayerCallbackQueue.remove(callback);
        }
    }
}

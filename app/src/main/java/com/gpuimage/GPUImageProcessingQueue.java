package com.gpuimage;

import android.opengl.GLSurfaceView;

import java.util.LinkedList;
import java.util.concurrent.Semaphore;

/**
 * Created by j on 28/11/2017.
 */

public class GPUImageProcessingQueue {

    public interface ThreadRequest {
        void request();
    }

    private LinkedList<Runnable> queue = new LinkedList<>();
    private long mThreadID = -1;
    private ThreadRequest mThreadRequest = null;

    private static GPUImageProcessingQueue self = null;

    public static synchronized GPUImageProcessingQueue sharedQueue() {
        if (self == null) {
            self = new GPUImageProcessingQueue();
        }
        return self;
    }

    public void setThreadID(long threadID) {
        mThreadID = threadID;
    }

    public void setThreadRequest(ThreadRequest threadRequest) {
        mThreadRequest = threadRequest;
    }

    public void runSyn(final Runnable runnable) {
        assert (mThreadRequest != null);
        if (runnable == null) return;

        final Semaphore synSem = new Semaphore(0);
        appendJob(new Runnable() {
            @Override
            public void run() {
                runnable.run();
                synSem.release();
            }
        });
        try {
            while (true) {
                mThreadRequest.request();
                Thread.sleep(10);
                if (synSem.tryAcquire()) break;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void runAsyn(Runnable runnable) {
        if (runnable == null) return;
        appendJob(runnable);
        if (mThreadRequest != null) mThreadRequest.request();
    }

    private void appendJob(Runnable runnable) {
        long currThreadID = Thread.currentThread().getId();
        if (currThreadID == mThreadID) {
            runnable.run();
        } else {
            synchronized (queue) {
                queue.addLast(runnable);
            }
        }
    }

    public void execute() {
        LinkedList<Runnable> tempQueue = new LinkedList<>();
        synchronized (queue) {
            tempQueue.addAll(queue);
            queue.clear();
        }
        while (!tempQueue.isEmpty()) {
            tempQueue.removeFirst().run();
        }
    }
}

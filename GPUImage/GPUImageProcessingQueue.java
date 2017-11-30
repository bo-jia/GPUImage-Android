package GPUImage;

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

    public void init(long threadID, ThreadRequest threadRequest) {
        mThreadID = threadID;
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
        mThreadRequest.request();
        try {
            synSem.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void runAsyn(Runnable runnable) {
        if (runnable == null) return;
        appendJob(runnable);
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
        boolean finished = false;
        do {
            Runnable job = null;
            synchronized (queue) {
                if (!queue.isEmpty()) {
                    job = queue.removeFirst();
                }
            }
            if (job == null) {
                return;
            } else {
                job.run();
            }
            synchronized (queue) {
                finished = queue.isEmpty();
            }
        } while (!finished);
    }


}

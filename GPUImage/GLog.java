package GPUImage;

import android.opengl.GLES20;
import android.util.Log;

/**
 * Created by j on 28/11/2017.
 */

public class GLog {
    public final static String TAG = "GPUImage";

    public static void v(String str) {
        Log.v(TAG, str);
    }

    public static void i(String str) {
        Log.i(TAG, str);
    }

    public static void e(String str) {
        Log.e(TAG, str);
    }

    public static void a(boolean assertion, String str) {
        if (!assertion) e(str);
        assert(assertion);
    }

    public static void checkFramebufferStatus() {
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        a(status == GLES20.GL_FRAMEBUFFER_COMPLETE, "Incomplete filter FBO: " + status);
    }
}

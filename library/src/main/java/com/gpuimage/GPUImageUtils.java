package com.gpuimage;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

/**
 * Created by j on 22/4/2018.
 */

public class GPUImageUtils {

    public final static Double TIMEINDEFINITE = Double.NaN;

    public static boolean TimeIsInDefinite(double timestamp) {
        return Double.compare(timestamp, TIMEINDEFINITE) == 0;
    }

    public static int setUnpackAlignment(int width) {
        int currAlignment[] = {0};
        GLES20.glGetIntegerv(GLES20.GL_UNPACK_ALIGNMENT, currAlignment, 0);
        // set unpack alignment
        if (width % 8 == 0) {
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 8);
        } else if (width % 4 == 0) {
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 4);
        } else if (width % 2 == 0) {
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 2);
        } else {
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        }
        return currAlignment[0];
    }

    public static Bitmap getGLRGBABitmap(Bitmap bitmap) {
        if (GLUtils.getType(bitmap) != GLES20.GL_UNSIGNED_BYTE || bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, false);
        }
        return bitmap;
    }
}

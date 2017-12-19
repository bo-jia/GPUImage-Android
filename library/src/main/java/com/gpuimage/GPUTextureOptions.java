package com.gpuimage;

import android.opengl.GLES20;

/**
 * Created by J on 6/1/16.
 */
public class GPUTextureOptions {
    public int minFilter 	    = GLES20.GL_LINEAR;
    public int magFilter 	    = GLES20.GL_LINEAR;
    public int wrapS		    = GLES20.GL_CLAMP_TO_EDGE;
    public int wrapT		    = GLES20.GL_CLAMP_TO_EDGE;
    public int internalFormat   = GLES20.GL_RGBA;
    public int format		    = GLES20.GL_RGBA;
    public int type			    = GLES20.GL_UNSIGNED_BYTE;

    public GPUTextureOptions() {}

    public GPUTextureOptions(GPUTextureOptions textureOptions) {
        minFilter = textureOptions.minFilter;
        magFilter = textureOptions.magFilter;
        wrapS = textureOptions.wrapS;
        wrapT = textureOptions.wrapT;
        internalFormat = textureOptions.internalFormat;
        format = textureOptions.format;
        type = textureOptions.type;
    }
}

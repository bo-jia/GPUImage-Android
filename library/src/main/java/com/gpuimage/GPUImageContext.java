package com.gpuimage;

import android.opengl.EGLContext;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.view.SurfaceView;

import com.gpuimage.eglutils.EglCore;

import java.util.HashMap;

/**
 * Created by j on 21/9/2017.
 */

public class GPUImageContext {
    public enum GPUImageRotationMode {
        kGPUImageNoRotation,
        kGPUImageRotateLeft,
        kGPUImageRotateRight,
        kGPUImageFlipVertical,
        kGPUImageFlipHorizonal,
        kGPUImageRotateRightFlipVertical,
        kGPUImageRotateRightFlipHorizontal,
        kGPUImageRotate180
    }

    private static int maxTextureSize = -1;
    private static int maxTextureUnits = -1;
    private static int maxVaryingVectors = -1;

    private EGLSurface mOffScreen;
    private EglCore mContext;

    private EglCore mSharedContext = null;
    private HashMap<String, GLProgram> mShaderProgramCache;

    protected GPUImageFramebufferCache mFramebufferCache;

    public GLProgram currentShaderProgram = null;

    private GDispatchQueue mContextQueue;

    private static GPUImageContext mSharedImageProcessingContext = null;

    public static synchronized GPUImageContext sharedImageProcessingContext() {
        if (mSharedImageProcessingContext == null) {
            mSharedImageProcessingContext = new GPUImageContext();
        }
        return mSharedImageProcessingContext;
    }

    public static GDispatchQueue sharedContextQueue() {
        return sharedImageProcessingContext().contextQueue();
    }

    public static void setActiveShaderProgram(GLProgram shaderProgram) {
        GPUImageContext sharedContext = sharedImageProcessingContext();
        sharedContext.setContextShaderProgram(shaderProgram);
    }

    public static GPUImageFramebufferCache sharedFramebufferCache() {
        return sharedImageProcessingContext().framebufferCache();
    }

    public static void useImageProcessingContext() {
        sharedImageProcessingContext().useAsCurrentContext();
    }

    public static void useImageProcessingContext(EGLSurface eglSurface) {
        sharedImageProcessingContext().useAsCurrentContext(eglSurface);
    }

    public GPUImageContext() {
        mContextQueue = new GDispatchQueue("com.gpuimage.openGLESContextQueue");
        mShaderProgramCache = new HashMap<>();
    }

    public void getParameters() {
        int param[] = new int[3];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, param, 0);
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_IMAGE_UNITS, param, 1);
        GLES20.glGetIntegerv(GLES20.GL_MAX_VARYING_VECTORS, param, 2);

        maxTextureSize = param[0];
        maxTextureUnits = param[1];
        maxVaryingVectors = param[2];
    }

    public void setContextShaderProgram(GLProgram shaderProgram) {
        EglCore imageProcessingContext = context();
        if (!imageProcessingContext.isCurrent()) {
            imageProcessingContext.makeCurrent(mOffScreen);
            if (currentShaderProgram == shaderProgram) {
                shaderProgram.use();
            }
        }
        if (currentShaderProgram != shaderProgram) {
            currentShaderProgram = shaderProgram;
            shaderProgram.use();
        }
    }

    public static int maximumTextureSizeForThisDevice() {
        if (maxTextureSize < 0) {
            useImageProcessingContext();
            int param[] = new int[1];
            GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, param, 0);
            maxTextureSize = param[0];
        }
        return maxTextureSize;
    }

    public static GSize sizeThatFitsWithinATextureForSize(GSize inputSize) {
        int maxTextureSize = maximumTextureSizeForThisDevice();
        if (inputSize.width < maxTextureSize && inputSize.height < maxTextureSize) {
            return inputSize;
        }

        GSize adjustedSize = new GSize();
        if (inputSize.width > inputSize.height)
        {
            adjustedSize.width  = maxTextureSize;
            adjustedSize.height = maxTextureSize * inputSize.height / inputSize.width;
        }
        else
        {
            adjustedSize.height = maxTextureSize;
            adjustedSize.width  = maxTextureSize * inputSize.width / inputSize.height;
        }
        return adjustedSize;
    }

    public GLProgram programForShaders(String vertexShaderString, String fragmentShaderString) {
        String lookupKeyForShaderProgram = "V: " + vertexShaderString + " - F: " + fragmentShaderString;
        GLProgram programFromCache = mShaderProgramCache.get(lookupKeyForShaderProgram);
        if (programFromCache == null) {
            programFromCache = new GLProgram(vertexShaderString, fragmentShaderString);
            mShaderProgramCache.put(lookupKeyForShaderProgram, programFromCache);
        }
        return programFromCache;
    }

    private EglCore createContext() {
        EglCore eglCore = new EglCore(mSharedContext == null ? null : mSharedContext.getEGLContext(), 0);
        mOffScreen = eglCore.createOffscreenSurface(1,1);
        return eglCore;
    }

    public GDispatchQueue contextQueue() {
        return mContextQueue;
    }

    public GPUImageFramebufferCache framebufferCache() {
        if (mFramebufferCache == null) {
            mFramebufferCache = new GPUImageFramebufferCache();
        }
        return mFramebufferCache;
    }

    public void useAsCurrentContext(EGLSurface eglSurface) {
        EglCore imageProcessingContext = context();
        if (!imageProcessingContext.isCurrent(eglSurface)) {
            imageProcessingContext.makeCurrent(eglSurface);
        }
    }

    public void useAsCurrentContext() {
        EglCore imageProcessingContext = context();
        if (!imageProcessingContext.isCurrent())
        {
            imageProcessingContext.makeCurrent(mOffScreen);
        }
    }

    public EglCore context() {
        if (mContext == null) {
            mContext = createContext();
            mContext.makeCurrent(mOffScreen);
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        }
        return mContext;
    }

    public void swapBufferForDisply(EGLSurface eglSurface) {
        boolean swap = mContext.swapBuffers(eglSurface);
        if (!swap) {
            GLog.e("swapBufferForDisply error");
        }
    }

    public void useSharedContext(EglCore sharedContext) {
        mSharedContext = sharedContext;
    }
}

package com.gpuimage;

import android.opengl.GLES20;

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

    public int maxTextureSize;
    public int maxTextureUnits;
    public int maxVaryingVectors;

    private HashMap<String, GLProgram> mShaderProgramCache = new HashMap<>();

    public GPUImageFramebufferCache framebufferCache = new GPUImageFramebufferCache();

    public GLProgram currentShaderProgram = null;

    private static GPUImageContext self = null;

    public static synchronized GPUImageContext sharedContext() {
        if (self == null) {
            self = new GPUImageContext();
        }
        return self;
    }

    private GPUImageContext() {}

    public void getParameters() {
        int param[] = new int[3];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, param, 0);
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_IMAGE_UNITS, param, 1);
        GLES20.glGetIntegerv(GLES20.GL_MAX_VARYING_VECTORS, param, 2);

        maxTextureSize = param[0];
        maxTextureUnits = param[1];
        maxVaryingVectors = param[2];
    }

    public void setActiveShaderProgram(GLProgram shaderProgram) {
        if (currentShaderProgram != shaderProgram) {
            currentShaderProgram = shaderProgram;
            shaderProgram.use();
        }
    }

    public GSize sizeThatFitsWithinATextureForSize(GSize inputSize) {
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


}

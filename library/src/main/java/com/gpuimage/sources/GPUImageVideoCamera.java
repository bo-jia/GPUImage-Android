package com.gpuimage.sources;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.gpuimage.GDispatchQueue;
import com.gpuimage.GLProgram;
import com.gpuimage.GLog;
import com.gpuimage.GPUImageContext;
import com.gpuimage.GPUImageFilter;
import com.gpuimage.GPUImageInput;
import com.gpuimage.GSize;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.opengles.GL10;

/**
 * @ClassName
 * @Description
 * @Author danny
 * @Date 2017/12/24 13:39
 */
public class GPUImageVideoCamera extends GPUImageOutput {
    protected final String TAG = getClass().getSimpleName();

    private static class SingletonHolder {
        private static final GPUImageVideoCamera INSTANCE = new GPUImageVideoCamera();
    }

    public static final GPUImageVideoCamera getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public static final String kGPUImageOES2ViewFShader =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "varying highp vec2 textureCoordinate;\n" +
                    " \n" +
                    "uniform samplerExternalOES inputImageTexture;\n" +
                    " \n" +
                    "void main()\n" +
                    "{\n" +
                    "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                    "}";

    private int mTextureUniform;
    private double mProcessingFrameTime;

    public static int mOESTexture = -1;

    GLProgram mOESProgram;
    private int mPositionAttrib, mTextureCoordinateAttrib;
    private ByteBuffer mTextureCoordinatesBuffer;
    private ByteBuffer mVerticesBuffer;

    private GSize mOutputTextureSize = new GSize();

    private GPUImageVideoCamera() {
        mVerticesBuffer = ByteBuffer
                .allocateDirect(GPUImageFilter.imageVertices.length * 4)
                .order(ByteOrder.nativeOrder());
        mVerticesBuffer.asFloatBuffer().put(GPUImageFilter.imageVertices);

        mTextureCoordinatesBuffer = ByteBuffer
                .allocateDirect(GPUImageFilter.noRotationTextureCoordinates.length * 4)
                .order(ByteOrder.nativeOrder());
        mTextureCoordinatesBuffer.asFloatBuffer().put(GPUImageFilter.noRotationTextureCoordinates);
        initProgram();
    }

    private void initProgram() {
        GDispatchQueue.runAsynchronouslyOnVideoProcessingQueue(() -> {
            GLog.v("camera initProgram");
            GPUImageContext.useImageProcessingContext();
            mOESProgram = GPUImageContext.sharedImageProcessingContext().programForShaders(GPUImageFilter.kGPUImageVertexShaderString, kGPUImageOES2ViewFShader);
            if (mOESProgram.initialized) {
                if (!mOESProgram.link()) {
                    String progLog = mOESProgram.programLog;
                    GLog.e("Program link log: " + progLog);
                    String fragLog = mOESProgram.fragmentShaderLog;
                    GLog.e("Fragment shader compile log: " + fragLog);
                    String vertLog = mOESProgram.vertexShaderLog;
                    GLog.e("Vertex shader compile log: " + vertLog);
                    mOESProgram = null;
                    GLog.a(false, "Filter shader link failed");
                }
            }
            GPUImageContext.setActiveShaderProgram(mOESProgram);
            mPositionAttrib = mOESProgram.attributeIndex("position");
            mTextureCoordinateAttrib = mOESProgram.attributeIndex("inputTextureCoordinate");
            mTextureUniform = mOESProgram.uniformIndex("inputImageTexture");
            GLES20.glEnableVertexAttribArray(mPositionAttrib);
            GLES20.glEnableVertexAttribArray(mTextureCoordinateAttrib);

            GLog.v("camera initProgram end");
        });
    }

    public void resetInputSize(final GSize size) {
        GDispatchQueue.runAsynchronouslyOnVideoProcessingQueue(() -> {
            mInputTextureSize = new GSize(size);
            int width = mInputTextureSize.width, height = mInputTextureSize.height;
            GLog.v(" resetInputSize w: " + width + " h:" + height);

            mTextureCoordinatesBuffer.asFloatBuffer().put(GPUImageFilter.noRotationTextureCoordinates);
            mOutputTextureSize = new GSize(mInputTextureSize.width, mInputTextureSize.height);
        });
    }


    public void setOESTexture(int mOESTexture) {
        mOESTexture = mOESTexture;
    }

    public void process(final double frameTime) {
        GDispatchQueue.runAsynchronouslyOnVideoProcessingQueue(() -> {
            mProcessingFrameTime = frameTime;

            GPUImageContext.setActiveShaderProgram(mOESProgram);

            mOutputFramebuffer = GPUImageContext.sharedFramebufferCache().fetchFramebuffer(mOutputTextureSize, false);
            mOutputFramebuffer.activateFramebuffer();
            GLog.checkFramebufferStatus();

            GLES20.glClearColor(0.3f, 0.3f, 0.6f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            GLog.v("process mOESTexture:  " + mOESTexture);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mOESTexture);
            GLES20.glUniform1i(mTextureUniform, 0);

            GLES20.glVertexAttribPointer(mPositionAttrib, 2, GLES20.GL_FLOAT, false, 0,
                    mVerticesBuffer);
            GLES20.glVertexAttribPointer(mTextureCoordinateAttrib, 2, GLES20.GL_FLOAT, false, 0,
                    mTextureCoordinatesBuffer);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            //Bitmap bitmap = mOutputFramebuffer.newBitmapFromFramebufferContents();
            //GLog.writeBitmap(bitmap, "sdfsfsf");

            for (GPUImageInput currentTarget : mTargets) {
                int indexOfObject = mTargets.indexOf(currentTarget);
                int targetTextureIndex = mTargetTextureIndices.get(indexOfObject);

                currentTarget.setInputSize(mOutputTextureSize, targetTextureIndex);
                currentTarget.setInputFramebuffer(mOutputFramebuffer, targetTextureIndex);
            }

            mOutputFramebuffer.unlock();

            for (GPUImageInput currentTarget : mTargets) {
                int indexOfObject = mTargets.indexOf(currentTarget);
                int targetTextureIndex = mTargetTextureIndices.get(indexOfObject);
                currentTarget.newFrameReadyAtTime(frameTime, targetTextureIndex);
            }
        });
    }

    public static int genOESTexture() {
        GLog.v("genOESTexture 1 ");
        GDispatchQueue.runSynchronouslyOnVideoProcessingQueue(() -> {
            GPUImageContext.useImageProcessingContext();
            GLog.v("genOESTexture 2 ");
            int[] texture = new int[1];
//            GLES20.glGenTextures(1, texture, 0);
//            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
//            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
//                    GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
//            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
//                    GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
//            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
//                    GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
//            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
//                    GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
//            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_2D, 0);

            GLES20.glGenTextures(1, texture, 0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

            mOESTexture = texture[0];
        });
        GLog.v("genOESTexture 3 , mOESTexture: " + mOESTexture);
        return mOESTexture;
    }

    public static int genOESTexture1() {
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        mOESTexture = tex[0];
        GLog.v("genOESTexture  , mOESTexture: " + mOESTexture);

        return mOESTexture;
    }
}

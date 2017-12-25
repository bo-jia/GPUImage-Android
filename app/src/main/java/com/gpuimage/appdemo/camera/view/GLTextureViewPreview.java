/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gpuimage.appdemo.camera.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.gpuimage.GDispatchQueue;
import com.gpuimage.appdemo.R;
import com.gpuimage.appdemo.utils.LogUtil;
import com.gpuimage.outputs.GPUImageView;
import com.gpuimage.sources.GPUImageVideoCamera;

@TargetApi(14)
public class GLTextureViewPreview extends PreviewImpl {
    protected final String TAG = getClass().getSimpleName();
    private final GPUImageView mGPUImageView;
    private int mDisplayOrientation;

    private int mOESTextureId;
    private SurfaceTexture mOESSurfaceTexture;
    private OESSurfaceTextureListener mOESSurfaceTextureListener = new OESSurfaceTextureListener();

    GLTextureViewPreview(Context context, ViewGroup parent) {
        mOESTextureId = GPUImageVideoCamera.genOESTexture();
        mOESSurfaceTexture = new SurfaceTexture(mOESTextureId);

        View view = View.inflate(context, R.layout.camera_gpuimageview, parent);
        mGPUImageView = view.findViewById(R.id.gpuimage_view);

        mGPUImageView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                LogUtil.v(TAG, "onSurfaceTextureAvailable start");
                setSize(width, height);
                configureTransform();
                dispatchSurfaceChanged();
                mOESSurfaceTexture.setOnFrameAvailableListener(mOESSurfaceTextureListener);
                LogUtil.v(TAG, "onSurfaceTextureAvailable end");
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
                setSize(width, height);
                configureTransform();
                dispatchSurfaceChanged();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                setSize(0, 0);
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    // This method is called only from Camera2.
    @TargetApi(15)
    @Override
    public void setBufferSize(int width, int height) {
        mGPUImageView.getSurfaceTexture().setDefaultBufferSize(width, height);
    }

    @Override
    public Surface getSurface() {
        //return new Surface(mGPUImageView.getSurfaceTexture());
        return new Surface(mOESSurfaceTexture);
    }

    @Override
    public SurfaceTexture getSurfaceTexture() {
        //return mGPUImageView.getSurfaceTexture();
        LogUtil.v(TAG, "getSurfaceTexture mOESSurfaceTexture : " + mOESSurfaceTexture + " mOESTextureId: " + mOESTextureId);
        return mOESSurfaceTexture;
    }

    @Override
    public View getView() {
        return mGPUImageView;
    }

    @Override
    public Class getOutputClass() {
        return SurfaceTexture.class;
    }

    @Override
    public void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
        configureTransform();
    }

    @Override
    public boolean isReady() {
        return mGPUImageView.getSurfaceTexture() != null;
    }

    /**
     * Configures the transform matrix for TextureView based on {@link #mDisplayOrientation} and
     * the surface size.
     */
    void configureTransform() {
        Matrix matrix = new Matrix();
        if (mDisplayOrientation % 180 == 90) {
            final int width = getWidth();
            final int height = getHeight();
            // Rotate the camera preview when the screen is landscape.
            matrix.setPolyToPoly(
                    new float[]{
                            0.f, 0.f, // top left
                            width, 0.f, // top right
                            0.f, height, // bottom left
                            width, height, // bottom right
                    }, 0,
                    mDisplayOrientation == 90 ?
                            // Clockwise
                            new float[]{
                                    0.f, height, // top left
                                    0.f, 0.f, // top right
                                    width, height, // bottom left
                                    width, 0.f, // bottom right
                            } : // mDisplayOrientation == 270
                            // Counter-clockwise
                            new float[]{
                                    width, 0.f, // top left
                                    width, height, // top right
                                    0.f, 0.f, // bottom left
                                    0.f, height, // bottom right
                            }, 0,
                    4);
        } else if (mDisplayOrientation == 180) {
            matrix.postRotate(180, getWidth() / 2, getHeight() / 2);
        }
        mGPUImageView.setTransform(matrix);
    }


    public class OESSurfaceTextureListener implements SurfaceTexture.OnFrameAvailableListener {
        public final String TAG = getClass().getSimpleName();

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            GDispatchQueue.runSynchronouslyOnVideoProcessingQueue(() -> {
                long t1, t2;
                t1 = System.currentTimeMillis();

                if (mOESSurfaceTexture != null) {
                    LogUtil.v(TAG, "onFrameAvailable1");
                    mOESSurfaceTexture.updateTexImage();
                    LogUtil.v(TAG, "onFrameAvailable2");

                    //mOESSurfaceTexture.getTransformMatrix(transformMatrix);
                }

                GPUImageVideoCamera.getInstance().process(t1);
                LogUtil.v(TAG, "onFrameAvailable3");

            });

        }
    }
}

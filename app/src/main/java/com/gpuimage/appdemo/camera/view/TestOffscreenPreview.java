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
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.OffScreenCanvas;
import com.chillingvan.canvasgl.glcanvas.BasicTexture;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.GLView;
import com.chillingvan.canvasgl.glview.texture.GLSurfaceTextureProducerView;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.canvasgl.glview.texture.gles.GLThread;
import com.gpuimage.appdemo.R;
import com.gpuimage.appdemo.utils.LogUtil;

@TargetApi(14)
public class TestOffscreenPreview extends PreviewImpl {
    protected final String TAG = getClass().getSimpleName();

    private int mDisplayOrientation;

    private CameraPreviewOffScreen cameraPreviewOffScreen;
    private SurfaceTexture mOESSurfaceTexture;

    private final ImageView imageView;

    TestOffscreenPreview(Context context, ViewGroup parent) {
        final View view = View.inflate(context, R.layout.camera_imageview_view, parent);
        imageView = view.findViewById(R.id.image_view);
        initCameraTexture();
    }

    private void initCameraTexture() {
        cameraPreviewOffScreen = new CameraPreviewOffScreen(400, 400);
        cameraPreviewOffScreen.start();

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraPreviewOffScreen.getDrawingBitmap(new Rect(0, 0, v.getWidth(), v.getHeight()), new GLView.GetDrawingCacheCallback() {
                    @Override
                    public void onFetch(Bitmap bitmap) {
                        imageView.setImageBitmap(bitmap);
                    }
                });

            }
        });

        cameraPreviewOffScreen.setOnCreateGLContextListener(new GLThread.OnCreateGLContextListener() {
            @Override
            public void onCreate(EglContextWrapper eglContext) {
            }
        });
        cameraPreviewOffScreen.setOnSurfaceTextureSet(new GLSurfaceTextureProducerView.OnSurfaceTextureSet() {
            @Override
            public void onSet(SurfaceTexture surfaceTexture, RawTexture surfaceTextureRelatedTexture) {
                surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        cameraPreviewOffScreen.requestRender();
                    }
                });

                mOESSurfaceTexture = surfaceTexture;
            }
        });
    }

    // This method is called only from Camera2.
    @TargetApi(15)
    @Override
    public void setBufferSize(int width, int height) {
    }

    @Override
    public Surface getSurface() {
        return new Surface(mOESSurfaceTexture);
    }

    @Override
    public SurfaceTexture getSurfaceTexture() {
        LogUtil.v(TAG, "getSurfaceTexture mOESSurfaceTexture : " + mOESSurfaceTexture );
        return mOESSurfaceTexture;
    }

    @Override
    public View getView() {
        //return mGPUImageView;
        return imageView;
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
       // return mGPUImageView.getSurfaceTexture() != null;
        return mOESSurfaceTexture != null;
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
        //mGPUImageView.setTransform(matrix);
    }


    public class CameraPreviewOffScreen extends OffScreenCanvas {

        public CameraPreviewOffScreen(int width, int height) {
            super(width, height);
            // !!!!!!!!!!!!!!!!!!!!!!!!!!!! important
            setProducedTextureTarget(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        }

        @Override
        protected void onGLDraw(ICanvasGL canvas, SurfaceTexture producedSurfaceTexture, RawTexture producedRawTexture, @Nullable SurfaceTexture outsideSharedSurfaceTexture, @Nullable BasicTexture outsideSharedTexture) {
            canvas.drawSurfaceTexture(producedRawTexture, producedSurfaceTexture, 0, 0, producedRawTexture.getWidth(), producedRawTexture.getHeight());
        }

    }
}

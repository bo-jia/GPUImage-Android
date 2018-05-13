//
// Created by Felix on 17/10/2017.
//

#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <android/bitmap.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include "opencv2/opencv.hpp"
#include <android/log.h>
#include "JNILog.h"

extern "C" {

/*
 * Image color format conversion
 * */

void NV122RGBAEla(unsigned char* nv12, int width, int height, int crop[4], int stride, int slice_height, unsigned char* rgba) {
    if (slice_height == 0) {
        slice_height = height;
    }

    int w = crop[2] - crop[0] + 1;
    int h = crop[3] - crop[1] + 1;

    cv::Mat nv_origin(slice_height * 3 / 2, stride, CV_8UC1, nv12);
    cv::Mat nv_compact;
    if (slice_height != h || stride != w) {
        nv_compact = cv::Mat(h * 3 / 2, w, CV_8UC1);
        // copy Y plane
        cv::Rect yRect(crop[0], crop[1], w, h);
        nv_origin(yRect).copyTo(nv_compact(cv::Rect(0, 0, w, h)));
        // copy uv plane
        cv::Rect nvRect(crop[0], crop[1] + slice_height, w, h/2);
        nv_origin(nvRect).copyTo(nv_compact(cv::Rect(0, h, w, h/2)));
    } else {
        nv_compact = nv_origin;
    }

    cv::Mat rgbaMat(h, w, CV_8UC4, rgba);
    cv::cvtColor(nv_compact, rgbaMat, CV_YUV2RGBA_NV12);
}

void NV122YUVEla(unsigned char* nv12, int width, int height, int crop[4], int stride, int slice_height, unsigned char* yuv) {
    if (slice_height == 0) {
        slice_height = height;
    }

    int w = crop[2] - crop[0] + 1;
    int h = crop[3] - crop[1] + 1;

    cv::Mat nv_origin(slice_height * 3 / 2, stride, CV_8UC1, nv12);

    cv::Rect yRect(crop[0], crop[1], w, h);
    cv::Rect nRect(crop[0], crop[1] + slice_height, w, h/2);

    // copy Y plane
    cv::Mat yuvMat(h * 3 / 2, w, CV_8UC1, yuv);
    nv_origin(yRect).copyTo(yuvMat(cv::Rect(0,0,w,h)));

    // copy UV plane
    cv::Mat nvMat(h/2, w, CV_8UC1);
    nv_origin(nRect).copyTo(nvMat(cv::Rect(0,0,w,h/2)));

    // split UV plane
    std::vector<cv::Mat> nvVec;
    cv::split(nvMat.reshape(2, h/4), nvVec);

    // copy U plane and V plane
    nvVec[0].copyTo(yuvMat(cv::Rect(0,h,w,h/4)));
    nvVec[1].copyTo(yuvMat(cv::Rect(0,h*5/4,w,h/4)));

}

void CompactNV12(unsigned char* nv12, int width, int height, int crop[4], int stride, int slice_height, unsigned char* nv12Cmp) {
    if (slice_height == 0) {
        slice_height = height;
    }

    int w = crop[2] - crop[0] + 1;
    int h = crop[3] - crop[1] + 1;

    cv::Mat nv_origin(slice_height * 3 / 2, stride, CV_8UC1, nv12);

    cv::Rect yRect(crop[0], crop[1], w, h);
    cv::Rect nRect(crop[0], crop[1] + slice_height, w, h/2);

    // copy Y plane
    cv::Mat nv_compact(h * 3 / 2, w, CV_8UC1, nv12Cmp);
    nv_origin(yRect).copyTo(nv_compact(cv::Rect(0,0,w,h)));

    // copy UV plane
    nv_origin(nRect).copyTo(nv_compact(cv::Rect(0,h,w,h/2)));
}

void ConvertToSemiPlanar(unsigned char* p0, int width, int height, unsigned char* p1) {

    cv::Mat u(height, width, CV_8UC1, p0);
    cv::Mat v(height, width, CV_8UC1, p0 + width * height);

    std::vector<cv::Mat> vec;
    vec.push_back(u);
    vec.push_back(v);

    cv::Mat nv(height, width, CV_8UC2, p1);
    cv::merge(vec, nv);
}

void YUV2RGBA(unsigned char* yuv, int width, int height, unsigned char* rgba) {
    cv::Mat yuvMat(height*3/2, width, CV_8UC1, yuv);
    cv::Mat rgbaMat(height, width, CV_8UC4, rgba);
    cv::cvtColor(yuvMat, rgbaMat, CV_YUV2RGBA_I420);
}

void NV122RGBA(unsigned char* nv12, int width, int height, unsigned char* rgba) {
    cv::Mat yuvMat(height*3/2, width, CV_8UC1, nv12);
    cv::Mat rgbaMat(height, width, CV_8UC4, rgba);
    cv::cvtColor(yuvMat, rgbaMat, CV_YUV2RGBA_NV12);
}

void Gray2RGBA(unsigned char* gray, int width, int height, unsigned char* rgba) {
    cv::Mat grayMat(height, width, CV_8UC1, gray);
    cv::Mat rgbaMat(height, width, CV_8UC4, rgba);
    cv::cvtColor(grayMat, rgbaMat, CV_GRAY2RGBA);
}

void NV122RGBA2(unsigned char* yuv, int sw, int sh, unsigned char* rgba, int dw, int dh) {
    assert(sw % 2 == 0 && sh % 2 == 0 && dw % 2 == 0 && dh % 2 == 0);

    // resize yuv on each plane
    cv::Mat mn = cv::Mat(sh,   sw,   CV_8UC1, yuv);
    cv::Mat mm = cv::Mat(sh/2, sw/2, CV_8UC2, yuv+sw*sh);

    unsigned char* dnv = (unsigned char*)malloc(dw * dh * 3 / 2);
    cv::Mat mdn = cv::Mat(dh,   dw,   CV_8UC1, dnv);
    cv::Mat mdm = cv::Mat(dh/2, dw/2, CV_8UC2, dnv+dw*dh);

    cv::resize(mn, mdn, cv::Size(dw,   dh));
    cv::resize(mm, mdm, cv::Size(dw/2, dh/2));

    // convert yuv to rgb
    cv::Mat mdnv(dh*3/2, dw, CV_8UC1, dnv);
    cv::Mat mrgb(dh, dw, CV_8UC4, rgba);
    cv::cvtColor(mdnv, mrgb, CV_YUV2RGBA_NV12);

    free(dnv);
}


JNIEXPORT void JNICALL
Java_com_gpuimage_cvutils_CVImageUtils_NV122RGBAEla(JNIEnv* env, jclass clzz, jbyteArray jsrcArray, jint width, jint height, jintArray jcropArray, jint stride, jint slice_height, jbyteArray jdstArray) {
    unsigned char *srcArray = (unsigned char *) env->GetPrimitiveArrayCritical(jsrcArray, 0);
    unsigned char *dstArray = (unsigned char *) env->GetPrimitiveArrayCritical(jdstArray, 0);
    int *cropArray = (int *) env->GetPrimitiveArrayCritical(jcropArray, 0);

    NV122RGBAEla(srcArray, width, height, cropArray, stride, slice_height, dstArray);

    env->ReleasePrimitiveArrayCritical(jsrcArray, srcArray, 0);
    env->ReleasePrimitiveArrayCritical(jdstArray, dstArray, 0);
    env->ReleasePrimitiveArrayCritical(jcropArray, cropArray, 0);
}

JNIEXPORT void JNICALL
Java_com_gpuimage_cvutils_CVImageUtils_NV122YUVEla(JNIEnv* env, jclass clzz, jbyteArray jsrcArray, jint width, jint height, jintArray jcropArray, jint stride, jint slice_height, jbyteArray jdstArray) {
    unsigned char *srcArray = (unsigned char *) env->GetPrimitiveArrayCritical(jsrcArray, 0);
    unsigned char *dstArray = (unsigned char *) env->GetPrimitiveArrayCritical(jdstArray, 0);
    int *cropArray = (int *) env->GetPrimitiveArrayCritical(jcropArray, 0);

    NV122YUVEla(srcArray, width, height, cropArray, stride, slice_height, dstArray);

    env->ReleasePrimitiveArrayCritical(jsrcArray, srcArray, 0);
    env->ReleasePrimitiveArrayCritical(jdstArray, dstArray, 0);
    env->ReleasePrimitiveArrayCritical(jcropArray, cropArray, 0);
}

JNIEXPORT void JNICALL
Java_com_gpuimage_cvutils_CVImageUtils_CompactNV12Ela(JNIEnv* env, jclass clzz, jbyteArray jsrcArray, jint width, jint height, jintArray jcropArray, jint stride, jint slice_height, jbyteArray jdstArray) {
    unsigned char *srcArray = (unsigned char *) env->GetPrimitiveArrayCritical(jsrcArray, 0);
    unsigned char *dstArray = (unsigned char *) env->GetPrimitiveArrayCritical(jdstArray, 0);
    int *cropArray = (int *) env->GetPrimitiveArrayCritical(jcropArray, 0);

    CompactNV12(srcArray, width, height, cropArray, stride, slice_height, dstArray);

    env->ReleasePrimitiveArrayCritical(jsrcArray, srcArray, 0);
    env->ReleasePrimitiveArrayCritical(jdstArray, dstArray, 0);
    env->ReleasePrimitiveArrayCritical(jcropArray, cropArray, 0);
}

JNIEXPORT void JNICALL
Java_com_gpuimage_cvutils_CVImageUtils_ConvertToSemiPlanar(JNIEnv* env, jclass clzz, jbyteArray juvArray, jint width, jint height) {
    unsigned char *uvArray = (unsigned char *) env->GetPrimitiveArrayCritical(juvArray, 0);

    unsigned char *nv = (unsigned char *)malloc(width * height / 2);
    ConvertToSemiPlanar(uvArray + width * height, width / 2, height / 2, nv);
    memcpy(uvArray + width * height, nv, width * height / 2);

    env->ReleasePrimitiveArrayCritical(juvArray, uvArray, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_com_gpuimage_cvutils_CVImageUtils_YUV2RGBA(JNIEnv* env, jclass clzz, jbyteArray jsrcArray, jint width, jint height, jbyteArray jdstArray) {
    unsigned char *srcArray = (unsigned char *) env->GetPrimitiveArrayCritical(jsrcArray, 0);
    unsigned char *dstArray = (unsigned char *) env->GetPrimitiveArrayCritical(jdstArray, 0);

    YUV2RGBA(srcArray, width, height, dstArray);

    env->ReleasePrimitiveArrayCritical(jsrcArray, srcArray, 0);
    env->ReleasePrimitiveArrayCritical(jdstArray, dstArray, 0);
}

JNIEXPORT void JNICALL
Java_com_gpuimage_cvutils_CVImageUtils_NV122RGBA(JNIEnv* env, jclass clzz, jbyteArray jsrcArray, jint width, jint height, jbyteArray jdstArray) {
    unsigned char *srcArray = (unsigned char *) env->GetPrimitiveArrayCritical(jsrcArray, 0);
    unsigned char *dstArray = (unsigned char *) env->GetPrimitiveArrayCritical(jdstArray, 0);

    NV122RGBA(srcArray, width, height, dstArray);

    env->ReleasePrimitiveArrayCritical(jsrcArray, srcArray, 0);
    env->ReleasePrimitiveArrayCritical(jdstArray, dstArray, 0);
}

JNIEXPORT void JNICALL
Java_com_gpuimage_cvutils_CVImageUtils_NV122RGBA2(JNIEnv* env, jclass clzz, jbyteArray jsrcArray, jint sw, jint sh, jbyteArray jdstArray, jint dw, jint dh) {
    unsigned char *srcArray = (unsigned char *) env->GetPrimitiveArrayCritical(jsrcArray, 0);
    unsigned char *dstArray = (unsigned char *) env->GetPrimitiveArrayCritical(jdstArray, 0);

    NV122RGBA2(srcArray, sw, sh, dstArray, dw, dh);

    env->ReleasePrimitiveArrayCritical(jsrcArray, srcArray, 0);
    env->ReleasePrimitiveArrayCritical(jdstArray, dstArray, 0);
}

JNIEXPORT void JNICALL
Java_com_gpuimage_cvutils_CVImageUtils_Gray2RGBA(JNIEnv* env, jclass clzz, jbyteArray jsrcArray, jint width, jint height, jint offset, jbyteArray jdstArray) {
    unsigned char *srcArray = (unsigned char *) env->GetPrimitiveArrayCritical(jsrcArray, 0);
    unsigned char *dstArray = (unsigned char *) env->GetPrimitiveArrayCritical(jdstArray, 0);

    Gray2RGBA(srcArray + offset, width, height, dstArray);

    env->ReleasePrimitiveArrayCritical(jsrcArray, srcArray, 0);
    env->ReleasePrimitiveArrayCritical(jdstArray, dstArray, 0);
}

JNIEXPORT void JNICALL
Java_com_gpuimage_cvutils_CVImageUtils_RotateClockwise(JNIEnv* env, jclass clzz, jbyteArray jsrcArray, jint width, jint height, jint rotation) {
    unsigned char *srcArray = (unsigned char *) env->GetPrimitiveArrayCritical(jsrcArray, 0);
    cv::Mat src(height, width, CV_8UC4, srcArray);
    switch (rotation) {
        case 90:
            cv::transpose(src, src);
            cv::flip(src, src, 1);
            break;
        case 180:
            cv::flip(src, src, -1);
            break;
        case 270:
            cv::transpose(src, src);
            cv::flip(src, src, -1);
            break;
    }
    env->ReleasePrimitiveArrayCritical(jsrcArray, srcArray, 0);
}


/**
 * Read Image from file to byte array in jni
 */
JNIEXPORT jobject JNICALL
Java_com_gpuimage_cvutils_CVImageUtils_ReadImage(JNIEnv* env, jclass clzz, jstring path, jintArray size, jint maxSize) {
    const char* pathPtr;
    pathPtr = env->GetStringUTFChars(path, 0);
    if(pathPtr == NULL) {
    }
    cv::Mat pic = cv::imread(pathPtr, CV_LOAD_IMAGE_UNCHANGED), preferredSizePic;
    env->ReleaseStringUTFChars(path, pathPtr);

    int w = pic.cols, h = pic.rows;
    if (maxSize > 0 && (pic.rows > maxSize || pic.cols > maxSize)) {
        w = pic.cols * maxSize / MAX(pic.rows, pic.cols);
        h = pic.rows * maxSize / MAX(pic.rows, pic.cols);
        cv::resize(pic, preferredSizePic, cv::Size(w, h), 0, 0, CV_INTER_CUBIC);
    } else {
        preferredSizePic = pic;
    }

    unsigned char *data = new unsigned char[w * h * 4];
    cv::Mat outPic(h, w, CV_8UC4, data);
    if (preferredSizePic.channels() == 1) {
        cv::cvtColor(preferredSizePic, outPic, CV_GRAY2RGBA);
    } else if (preferredSizePic.channels() == 3) {
        cv::cvtColor(preferredSizePic, outPic, CV_BGR2RGBA);
    } else {
        cv::cvtColor(preferredSizePic, outPic, CV_BGRA2RGBA);
    }

    int *sizePtr = (int *)env->GetPrimitiveArrayCritical(size, 0);
    sizePtr[0] = w;
    sizePtr[1] = h;
    env->ReleasePrimitiveArrayCritical(size, sizePtr, 0);

    return env->NewDirectByteBuffer(data, 0);
}

JNIEXPORT void JNICALL
Java_com_gpuimage_cvutils_CVImageUtils_ReleaseImage(JNIEnv* env, jclass clzz, jobject handler) {
    unsigned char * ptr = (unsigned char *) env->GetDirectBufferAddress(handler);
    if (ptr) {
        delete [] ptr;
    }
}
JNIEXPORT void JNICALL
Java_com_gpuimage_cvutils_CVImageUtils_GetData(JNIEnv* env, jclass clzz, jobject handler, jint sw, jint sh, jbyteArray data, jint dw, jint dh) {
    unsigned char * ptr = (unsigned char *) env->GetDirectBufferAddress(handler);
    unsigned char * dataPtr = (unsigned char *) env->GetPrimitiveArrayCritical(data, 0);

    cv::Mat src(sh, sw, CV_8UC4, ptr);
    cv::Mat dst(dh, dw, CV_8UC4, dataPtr);
    cv::resize(src, dst, cv::Size(dw, dh), 0, 0, CV_INTER_CUBIC);

    env->ReleasePrimitiveArrayCritical(data, dataPtr, 0);
}

/**
 * Load image in GPU
 */

JNIEXPORT void JNICALL
Java_com_gpuimage_cvutils_CVImageUtils_LoadBitmapForTexture(JNIEnv* env, jclass clzz, jobject bitmap, jint texWidth, jint texHeight) {
    AndroidBitmapInfo bmpInfo = {0};
    if (AndroidBitmap_getInfo(env, bitmap, &bmpInfo)!= ANDROID_BITMAP_RESULT_SUCCESS) {
        return;
    }
    unsigned char * data = NULL;
    if (AndroidBitmap_lockPixels(env, bitmap, (void * *) & data) != ANDROID_BITMAP_RESULT_SUCCESS) {
        return;
    }
    if (bmpInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        JNILogE("The format of bitmap is not equal with RGBA_8888!");
        AndroidBitmap_unlockPixels(env, bitmap);
        return;
    }

    cv::Mat pic(bmpInfo.height, bmpInfo.stride, CV_8UC4, data);

    if (pic.rows > texHeight || pic.cols > texWidth) {
        cv::Mat resizedPic;
        cv::resize(pic(cv::Rect(0, 0, bmpInfo.width, bmpInfo.height)), resizedPic, cv::Size(texWidth, texHeight), 0, 0, CV_INTER_CUBIC);
        pic = resizedPic;
    } else if (bmpInfo.stride != bmpInfo.width) {
        cv::Mat newPic;
        pic(cv::Rect(0, 0, bmpInfo.width, bmpInfo.height)).copyTo(newPic);
        pic = newPic;
    }
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, pic.cols, pic.rows, GL_RGBA, GL_UNSIGNED_BYTE, pic.data);

    AndroidBitmap_unlockPixels(env, bitmap);
}

JNIEXPORT void JNICALL
Java_com_gpuimage_cvutils_CVImageUtils_LoadCVPictureForTexture(JNIEnv* env, jclass clzz, jobject handler, jint picWidth, jint picHeight, jint texWidth, jint texHeight) {
    unsigned char * ptr = (unsigned char *) env->GetDirectBufferAddress(handler);

    cv::Mat pic(picHeight, picWidth, CV_8UC4, ptr);

    if (pic.rows > texHeight || pic.cols > texWidth) {
        cv::Mat resizedPic;
        cv::resize(pic, resizedPic, cv::Size(texWidth, texHeight), 0, 0, CV_INTER_CUBIC);
        pic = resizedPic;
    }

    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, pic.cols, pic.rows, GL_RGBA, GL_UNSIGNED_BYTE, pic.data);
}

}
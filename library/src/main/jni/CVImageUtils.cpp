//
// Created by Felix on 17/10/2017.
//

#include <jni.h>
#include <stdio.h>
#include <string.h>
#include "opencv2/opencv.hpp"
#include <android/log.h>

#define LOG_TAG "CVImageUtils"

// __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "%s", buffer);
extern "C" {

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


JNIEXPORT void JNICALL Java_com_gpuimage_cvutils_CVImageUtils_NV122RGBAEla(JNIEnv* env, jclass clzz, jbyteArray jsrcArray, jint width, jint height, jintArray jcropArray, jint stride, jint slice_height, jbyteArray jdstArray) {
    unsigned char *srcArray = (unsigned char *) env->GetPrimitiveArrayCritical(jsrcArray, 0);
    unsigned char *dstArray = (unsigned char *) env->GetPrimitiveArrayCritical(jdstArray, 0);
    int *cropArray = (int *) env->GetPrimitiveArrayCritical(jcropArray, 0);

    NV122RGBAEla(srcArray, width, height, cropArray, stride, slice_height, dstArray);

    env->ReleasePrimitiveArrayCritical(jsrcArray, srcArray, 0);
    env->ReleasePrimitiveArrayCritical(jdstArray, dstArray, 0);
    env->ReleasePrimitiveArrayCritical(jcropArray, cropArray, 0);
}

JNIEXPORT void JNICALL Java_com_gpuimage_cvutils_CVImageUtils_NV122YUVEla(JNIEnv* env, jclass clzz, jbyteArray jsrcArray, jint width, jint height, jintArray jcropArray, jint stride, jint slice_height, jbyteArray jdstArray) {
    unsigned char *srcArray = (unsigned char *) env->GetPrimitiveArrayCritical(jsrcArray, 0);
    unsigned char *dstArray = (unsigned char *) env->GetPrimitiveArrayCritical(jdstArray, 0);
    int *cropArray = (int *) env->GetPrimitiveArrayCritical(jcropArray, 0);

    NV122YUVEla(srcArray, width, height, cropArray, stride, slice_height, dstArray);

    env->ReleasePrimitiveArrayCritical(jsrcArray, srcArray, 0);
    env->ReleasePrimitiveArrayCritical(jdstArray, dstArray, 0);
    env->ReleasePrimitiveArrayCritical(jcropArray, cropArray, 0);
}

JNIEXPORT void JNICALL Java_com_gpuimage_cvutils_CVImageUtils_CompactNV12Ela(JNIEnv* env, jclass clzz, jbyteArray jsrcArray, jint width, jint height, jintArray jcropArray, jint stride, jint slice_height, jbyteArray jdstArray) {
    unsigned char *srcArray = (unsigned char *) env->GetPrimitiveArrayCritical(jsrcArray, 0);
    unsigned char *dstArray = (unsigned char *) env->GetPrimitiveArrayCritical(jdstArray, 0);
    int *cropArray = (int *) env->GetPrimitiveArrayCritical(jcropArray, 0);

    CompactNV12(srcArray, width, height, cropArray, stride, slice_height, dstArray);

    env->ReleasePrimitiveArrayCritical(jsrcArray, srcArray, 0);
    env->ReleasePrimitiveArrayCritical(jdstArray, dstArray, 0);
    env->ReleasePrimitiveArrayCritical(jcropArray, cropArray, 0);
}

JNIEXPORT void JNICALL Java_com_gpuimage_cvutils_CVImageUtils_YUV2RGBA(JNIEnv* env, jclass clzz, jbyteArray jsrcArray, jint width, jint height, jbyteArray jdstArray) {
    unsigned char *srcArray = (unsigned char *) env->GetPrimitiveArrayCritical(jsrcArray, 0);
    unsigned char *dstArray = (unsigned char *) env->GetPrimitiveArrayCritical(jdstArray, 0);

    YUV2RGBA(srcArray, width, height, dstArray);

    env->ReleasePrimitiveArrayCritical(jsrcArray, srcArray, 0);
    env->ReleasePrimitiveArrayCritical(jdstArray, dstArray, 0);
}

JNIEXPORT void JNICALL Java_com_gpuimage_cvutils_CVImageUtils_NV122RGBA(JNIEnv* env, jclass clzz, jbyteArray jsrcArray, jint width, jint height, jbyteArray jdstArray) {
    unsigned char *srcArray = (unsigned char *) env->GetPrimitiveArrayCritical(jsrcArray, 0);
    unsigned char *dstArray = (unsigned char *) env->GetPrimitiveArrayCritical(jdstArray, 0);

    NV122RGBA(srcArray, width, height, dstArray);

    env->ReleasePrimitiveArrayCritical(jsrcArray, srcArray, 0);
    env->ReleasePrimitiveArrayCritical(jdstArray, dstArray, 0);
}

JNIEXPORT void JNICALL Java_com_gpuimage_cvutils_CVImageUtils_Gray2RGBA(JNIEnv* env, jclass clzz, jbyteArray jsrcArray, jint width, jint height, jint offset, jbyteArray jdstArray) {
    unsigned char *srcArray = (unsigned char *) env->GetPrimitiveArrayCritical(jsrcArray, 0);
    unsigned char *dstArray = (unsigned char *) env->GetPrimitiveArrayCritical(jdstArray, 0);

    Gray2RGBA(srcArray + offset, width, height, dstArray);

    env->ReleasePrimitiveArrayCritical(jsrcArray, srcArray, 0);
    env->ReleasePrimitiveArrayCritical(jdstArray, dstArray, 0);
}

}
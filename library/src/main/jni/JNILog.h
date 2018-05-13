
#ifndef GPUIMAGE_JNILOG_H
#define GPUIMAGE_JNILOG_H

#include <android/log.h>
#define JNILogTag "GPUImage_JNI"
#define JNILogV(...)  __android_log_print(ANDROID_LOG_VERBOSE, JNILogTag,__VA_ARGS__)
#define JNILogE(...)  __android_log_print(ANDROID_LOG_ERROR,   JNILogTag,__VA_ARGS__)

#endif //GPUIMAGE_JNILOG_H

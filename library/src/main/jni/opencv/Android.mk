LOCAL_PATH := $(call my-dir)
OPENCV_ARMV7_3RDPARTY_LIBPATH := $(call my-dir)/../../../../thirdparty/opencv-3.0.0/sdk/native/3rdparty/libs/armeabi-v7a
OPENCV_ARMV7_NATIVE_LIBPATH := $(call my-dir)/../../../../thirdparty/opencv-3.0.0/sdk/native/libs/armeabi-v7a

OPENCV_ARMV5_3RDPARTY_LIBPATH := $(call my-dir)/../../../../thirdparty/opencv-3.0.0/sdk/native/3rdparty/libs/armeabi
OPENCV_ARMV5_NATIVE_LIBPATH := $(call my-dir)/../../../../thirdparty/opencv-3.0.0/sdk/native/libs/armeabi

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
OPENCV_NATIVE_LIBPATH := $(OPENCV_ARMV7_NATIVE_LIBPATH)
OPENCV_3RDPARTY_LIBPATH := $(OPENCV_ARMV7_3RDPARTY_LIBPATH)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_java3
LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_java3.so
#$(warning $(LOCAL_SRC_FILES))
include $(PREBUILT_SHARED_LIBRARY)
endif

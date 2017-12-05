LOCAL_PATH := $(call my-dir)
OPENCV_ARMV7_3RDPARTY_LIBPATH := $(call my-dir)/../../../../libs/opencv-3.0.0/sdk/native/3rdparty/libs/armeabi-v7a
OPENCV_ARMV7_NATIVE_LIBPATH := $(call my-dir)/../../../../libs/opencv-3.0.0/sdk/native/libs/armeabi-v7a

OPENCV_ARMV5_3RDPARTY_LIBPATH := $(call my-dir)/../../../../libs/opencv-3.0.0/sdk/native/3rdparty/libs/armeabi
OPENCV_ARMV5_NATIVE_LIBPATH := $(call my-dir)/../../../../libs/opencv-3.0.0/sdk/native/libs/armeabi

OPENCV_X86_3RDPARTY_LIBPATH := $(call my-dir)/../../../../libs/opencv-3.0.0/sdk/native/3rdparty/libs/x86
OPENCV_X86_NATIVE_LIBPATH := $(call my-dir)/../../../../libs/opencv-3.0.0/sdk/native/libs/x86

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
OPENCV_NATIVE_LIBPATH := $(OPENCV_ARMV7_NATIVE_LIBPATH)
OPENCV_3RDPARTY_LIBPATH := $(OPENCV_ARMV7_3RDPARTY_LIBPATH)
endif
ifeq ($(TARGET_ARCH_ABI),armeabi)
OPENCV_NATIVE_LIBPATH := $(OPENCV_ARMV5_NATIVE_LIBPATH)
OPENCV_3RDPARTY_LIBPATH := $(OPENCV_ARMV5_3RDPARTY_LIBPATH)
endif
ifeq ($(TARGET_ARCH_ABI),x86)
OPENCV_NATIVE_LIBPATH := $(OPENCV_X86_NATIVE_LIBPATH)
OPENCV_3RDPARTY_LIBPATH := $(OPENCV_X86_3RDPARTY_LIBPATH)
endif

include $(CLEAR_VARS)
LOCAL_MODULE := IlmImf
LOCAL_SRC_FILES := $(OPENCV_3RDPARTY_LIBPATH)/libIlmImf.a
$(warning $(LOCAL_SRC_FILES))
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjasper
LOCAL_SRC_FILES := $(OPENCV_3RDPARTY_LIBPATH)/liblibjasper.a
$(warning $(LOCAL_SRC_FILES))
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjpeg
LOCAL_SRC_FILES := $(OPENCV_3RDPARTY_LIBPATH)/liblibjpeg.a
$(warning $(LOCAL_SRC_FILES))
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libwebp
LOCAL_SRC_FILES := $(OPENCV_3RDPARTY_LIBPATH)/liblibwebp.a
$(warning $(LOCAL_SRC_FILES))
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libpng
LOCAL_SRC_FILES := $(OPENCV_3RDPARTY_LIBPATH)/liblibpng.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libtiff
LOCAL_SRC_FILES := $(OPENCV_3RDPARTY_LIBPATH)/liblibtiff.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := tbb
LOCAL_SRC_FILES := $(OPENCV_3RDPARTY_LIBPATH)/libtbb.a
include $(PREBUILT_STATIC_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE := opencv_androidcamera
#LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_androidcamera.a
#include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_calib3d
LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_calib3d.a
include $(PREBUILT_STATIC_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE := opencv_contrib
#LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_contrib.a
#include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_core
LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_core.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_features2d
LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_features2d.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_flann
LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_flann.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_hal
LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_hal.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_highgui
LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_highgui.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_imgcodecs
LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_imgcodecs.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_imgproc
LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_imgproc.a
include $(PREBUILT_STATIC_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE := opencv_legacy
#LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_legacy.a
#include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_ml
LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_ml.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_objdetect
LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_objdetect.a
include $(PREBUILT_STATIC_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE := opencv_ocl
#LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_ocl.a
#include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_photo
LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_photo.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_shape
LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_shape.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_stitching
LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_stitching.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_superres
LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_superres.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_ts
LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_ts.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_video
LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_video.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_videoio
LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_videoio.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_videostab
LOCAL_SRC_FILES := $(OPENCV_NATIVE_LIBPATH)/libopencv_videostab.a
$(warning $(LOCAL_SRC_FILES))
include $(PREBUILT_STATIC_LIBRARY)
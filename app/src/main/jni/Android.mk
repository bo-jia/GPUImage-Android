LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := cvimageutils

OPENCV_INCDIR := $(LOCAL_PATH)/../../../libs/opencv-3.0.0/sdk/native/jni/include/

GLOBAL_C_INCLUDES := \
  $(OPENCV_INCDIR)/

LOCAL_SRC_FILES := \
  CVImageUtils.cpp \

LOCAL_DISABLE_FATAL_LINKER_WARNINGS := true
LOCAL_CPPFLAGS += -DANDROID -DBUILD_CROSS_PLATFORM -DFIXED_POINT -DARM -O3
LOCAL_CFLAGS += -DANDROID -DBUILD_CROSS_PLATFORM -DFIXED_POINT -DARM -O3
#swscale libspeex
LOCAL_STATIC_LIBRARIES := \
 opencv_objdetect opencv_imgcodecs \
 opencv_video opencv_calib3d opencv_features2d \
 opencv_flann opencv_highgui opencv_imgproc opencv_ml \
 opencv_photo opencv_stitching opencv_superres opencv_ts opencv_videostab opencv_core opencv_hal tbb \
 opencv_shape opencv_videoio \
 libjpeg libpng libtiff libjasper IlmImf libwebp

LOCAL_LDLIBS    := -lm -llog -lz -ljnigraphics
LOCAL_C_INCLUDES := $(GLOBAL_C_INCLUDES)

include $(BUILD_SHARED_LIBRARY)
#include $(BUILD_STATIC_LIBRARY)
##include $(BUILD_EXECUTABLE)

include $(call all-makefiles-under,$(LOCAL_PATH))



LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := cvimageutils

OPENCV_INCDIR := $(LOCAL_PATH)/../../../thirdparty/opencv-3.0.0/sdk/native/jni/include/

GLOBAL_C_INCLUDES := \
  $(OPENCV_INCDIR)/ \

LOCAL_SRC_FILES := \
  CVImageUtils.cpp \
  ByteArrayUtils.cpp \

LOCAL_DISABLE_FATAL_LINKER_WARNINGS := true
LOCAL_CPPFLAGS += -DANDROID -DBUILD_CROSS_PLATFORM -DFIXED_POINT -DARM -O3
LOCAL_CFLAGS += -DANDROID -DBUILD_CROSS_PLATFORM -DFIXED_POINT -DARM -O3

LOCAL_SHARED_LIBRARIES := opencv_java3
LOCAL_LDLIBS    := -lm -llog -lGLESv2 -lEGL -landroid -lz -ljnigraphics
LOCAL_C_INCLUDES := $(GLOBAL_C_INCLUDES)

include $(BUILD_SHARED_LIBRARY)
#include $(BUILD_STATIC_LIBRARY)
##include $(BUILD_EXECUTABLE)

include $(call all-makefiles-under,$(LOCAL_PATH))



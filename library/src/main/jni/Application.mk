APP_ABI      := armeabi-v7a
APP_STL      := gnustl_static
APP_CPPFLAGS += -frtti -std=c++11 -D__STDC_CONSTANT_MACROS -fexceptions
APP_CFLAGS += -Wno-error=format-security
#APP_MODULES  := cvimageutils

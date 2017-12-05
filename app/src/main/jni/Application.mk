APP_OPTIM    := release
APP_PLATFORM := android-9
APP_STL      := gnustl_static
APP_CPPFLAGS += -frtti
APP_CPPFLAGS += -fexceptions
APP_CPPFLAGS += -std=c++11
APP_CPPFLAGS += -D__STDC_CONSTANT_MACROS
APP_CFLAGS += -Wno-error=format-security
APP_ABI      := armeabi armeabi-v7a x86
APP_MODULES  := cvimageutils
NDK_TOOLCHAIN_VERSION := 4.9

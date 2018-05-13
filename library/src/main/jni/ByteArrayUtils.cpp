
#include <jni.h>
#include <cstddef>
#include <cstring>

extern "C" {

JNIEXPORT jobject JNICALL
Java_com_gpuimage_mediautils_GByteArrayUtils_newByteArray(JNIEnv *env, jclass clzz, jint size) {
    if (size <= 0) {
        return NULL;
    }
    unsigned char* data = new unsigned char[size];
    return env->NewDirectByteBuffer(data, 0);
}

JNIEXPORT void JNICALL
Java_com_gpuimage_mediautils_GByteArrayUtils_setBytes(JNIEnv *env, jclass clzz, jobject dst, jbyteArray src, jint offset, jint length) {

    unsigned char * dstPtr = (unsigned char *) env->GetDirectBufferAddress(dst);
    unsigned char * srcPtr = (unsigned char *) env->GetPrimitiveArrayCritical(src, NULL);
    memcpy(dstPtr, srcPtr+offset, length);
    env->ReleasePrimitiveArrayCritical(src, srcPtr, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_com_gpuimage_mediautils_GByteArrayUtils_getBytes(JNIEnv *env, jclass clzz, jbyteArray dst, jint offset, jint length, jobject src) {

    unsigned char * srcPtr = (unsigned char *) env->GetDirectBufferAddress(src);
    unsigned char * dstPtr = (unsigned char *) env->GetPrimitiveArrayCritical(dst, NULL);
    memcpy(dstPtr+offset, srcPtr, length);
    env->ReleasePrimitiveArrayCritical(dst, dstPtr, 0);
}

JNIEXPORT void JNICALL
Java_com_gpuimage_mediautils_GByteArrayUtils_freeByteArray(JNIEnv *env, jclass clzz, jobject handler) {
    unsigned char * ptr = (unsigned char *) env->GetDirectBufferAddress(handler);
    if (ptr) {
        delete [] ptr;
    }
}

}

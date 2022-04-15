#include <jni.h>
#include <string>
#include <cstdio>
#include <android/log.h>

#pragma clang diagnostic push
#pragma ide diagnostic ignored "err_typecheck_invalid_operands"
#define APPNAME "app jni"
#define LOGD(FORMAT, VALUE) __android_log_print(ANDROID_LOG_DEBUG , APPNAME, FORMAT, VALUE);

extern "C" JNIEXPORT void

JNICALL
Java_com_selfsimilartech_fractaleye_VideoEncoder_yuvMap(
        JNIEnv *env,
        jobject /* this */,
        jint width,
        jint height,
        jint uvPixelStride,
        jbyteArray inputArray,
        jobject yBufferObj,
        jobject uBufferObj,
        jobject vBufferObj
) {


    jboolean isCopy;
    jbyte *input = env->GetByteArrayElements(inputArray, &isCopy);

    auto *yBuffer = static_cast<jbyte *>(env->GetDirectBufferAddress(yBufferObj));
    auto *uBuffer = static_cast<jbyte *>(env->GetDirectBufferAddress(uBufferObj));
    auto *vBuffer = static_cast<jbyte *>(env->GetDirectBufferAddress(vBufferObj));

    int yIndex = 0;
    int uIndex = 0;
    int vIndex = 0;

    for (int n = 0; n < width*height; n++) {

        int i = n % width;
        int j = n/width;

        yBuffer[yIndex] = input[4*n];
        yIndex++;

        if (i % 2 == 0 && j % 2 == 0) {  // 4:2:0
        // if (i % 4 == 0) {  // 4:1:1
            uBuffer[uIndex] = input[4*n + 1];
            vBuffer[vIndex] = input[4*n + 2];
            uIndex += uvPixelStride;
            vIndex += uvPixelStride;
        }

    }



}
#pragma clang diagnostic pop
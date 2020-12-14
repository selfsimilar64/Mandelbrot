#include <jni.h>
#include <string>
#include <stdio.h>
#include <android/log.h>
#include "toojpeg.h"

#pragma clang diagnostic push
#pragma ide diagnostic ignored "err_typecheck_invalid_operands"
#define APPNAME "app jni"
#define LOGD(FORMAT, VALUE) __android_log_print(ANDROID_LOG_DEBUG , APPNAME, FORMAT, VALUE);

unsigned char* imOut;
int index = 0;

void writeByte(unsigned char byte) {
    imOut[index] = byte;
    index++;
}

extern "C" JNIEXPORT jint
JNICALL
Java_com_selfsimilartech_fractaleye_FractalRenderer_compressNative(
        JNIEnv *env,
        jobject /* this */,
        jbyteArray imIn,
        int width,
        int height) {

    jboolean isCopy;
    imOut = reinterpret_cast<unsigned char*>(env->GetByteArrayElements(imIn, &isCopy));
    // imOut = new unsigned char[width*height*3];

//    for (auto y = 0; y < height; y++)
//        for (auto x = 0; x < width; x++)
//        {
//            // memory location of current pixel
//            auto offset = (y*width + x)*3;
//
//            // red and green fade from 0 to 255, blue is always 127
//            imOut[offset    ] = 255 * x / width;
//            imOut[offset + 1] = 255 * y / height;
//            imOut[offset + 2] = 127;
//
//        }

    int j = 0;
    for (int i = 0; i < width*height*4; i++) {
        if ((i + 1) % 4 == 0) i++;
        imOut[j] = imOut[i];
        j++;
    }

    const bool  isRGB       = true;                     // true = RGB image, else false = grayscale
    const auto  quality     = 100;                      // compression quality: 0 = worst, 100 = best, 80 to 90 are most often used
    const bool  downsample  = true;                     // false = save as YCbCr444 JPEG (better quality), true = YCbCr420 (smaller file)
    const char* comment     = "TooJpeg example image";  // arbitrary JPEG comment

    auto ok = TooJpeg::writeJpeg(writeByte, imOut, width, height, isRGB, quality, downsample, comment);

    env->SetByteArrayRegion(imIn, 0, index, reinterpret_cast<signed char*>(imOut));
    return index;

}

#pragma clang diagnostic pop
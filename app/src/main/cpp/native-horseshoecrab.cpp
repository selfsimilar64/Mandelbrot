#include <jni.h>
#include <string>
#include <stdio.h>
#include <android/log.h>
#include <omp.h>
#include "complexmath.h"

#pragma clang diagnostic push
#define APPNAME "app jni"
#define LOGD(FORMAT, VALUE) __android_log_print(ANDROID_LOG_DEBUG , APPNAME, FORMAT, VALUE);

extern "C" JNIEXPORT jfloatArray

JNICALL
Java_com_selfsimilartech_fractaleye_FractalRenderer_iterateHorseshoeCrabNative(
        JNIEnv *env,
        jobject, /* this */
        jobject data) {


    jclass dataClass            = env->GetObjectClass(data);
    jfieldID widthField         = env->GetFieldID(dataClass, "width",         "I");
    jfieldID heightField        = env->GetFieldID(dataClass, "height",        "I");
    jfieldID yStartField        = env->GetFieldID(dataClass, "yStart",        "I");
    jfieldID yEndField          = env->GetFieldID(dataClass, "yEnd"  ,        "I");
    jfieldID ratioField         = env->GetFieldID(dataClass, "aspectRatio",   "D");
    jfieldID bgScaleField       = env->GetFieldID(dataClass, "bgScale",       "D");
    jfieldID maxIterField       = env->GetFieldID(dataClass, "maxIter",       "J");
    jfieldID radiusField        = env->GetFieldID(dataClass, "escapeRadius",  "F");
    jfieldID zoomField          = env->GetFieldID(dataClass, "scale",         "D");
    jfieldID xCoordField        = env->GetFieldID(dataClass, "xCoord",        "D");
    jfieldID yCoordField        = env->GetFieldID(dataClass, "yCoord",        "D");
    jfieldID sinRotationField   = env->GetFieldID(dataClass, "sinRotation",   "D");
    jfieldID cosRotationField   = env->GetFieldID(dataClass, "cosRotation",   "D");
    jfieldID x0Field            = env->GetFieldID(dataClass, "x0",            "D");
    jfieldID y0Field            = env->GetFieldID(dataClass, "y0",            "D");
    jfieldID juliaModeField     = env->GetFieldID(dataClass, "juliaMode",     "Z");
    jfieldID jxField            = env->GetFieldID(dataClass, "jx",            "D");
    jfieldID jyField            = env->GetFieldID(dataClass, "jy",            "D");
    jfieldID p1xField           = env->GetFieldID(dataClass, "p1x",           "D");
    jfieldID p1yField           = env->GetFieldID(dataClass, "p1y",           "D");
    jfieldID p2xField           = env->GetFieldID(dataClass, "p2x",           "D");
    jfieldID p2yField           = env->GetFieldID(dataClass, "p2y",           "D");
    jfieldID p3xField           = env->GetFieldID(dataClass, "p3x",           "D");
    jfieldID p3yField           = env->GetFieldID(dataClass, "p3y",           "D");
    jfieldID p4xField           = env->GetFieldID(dataClass, "p4x",           "D");
    jfieldID p4yField           = env->GetFieldID(dataClass, "p4y",           "D");

    int width           = env->GetIntField(data, widthField);
    int height          = env->GetIntField(data, heightField);
    int yStart          = env->GetIntField(data, yStartField);
    int yEnd            = env->GetIntField(data, yEndField);
    double aspectRatio  = env->GetDoubleField(data, ratioField);
    double bgScale      = env->GetDoubleField(data, bgScaleField);
    int maxIter         = int(env->GetLongField(data, maxIterField));
    float escapeRadius  = env->GetFloatField(data, radiusField);
    double scale        = env->GetDoubleField(data, zoomField);
    double xCoord       = env->GetDoubleField(data, xCoordField);
    double yCoord       = env->GetDoubleField(data, yCoordField);
    double sinRotation  = env->GetDoubleField(data, sinRotationField);
    double cosRotation  = env->GetDoubleField(data, cosRotationField);
    bool juliaMode      = env->GetBooleanField(data, juliaModeField);
    complex z0 = complex {
            env->GetDoubleField(data, x0Field),
            env->GetDoubleField(data, y0Field)
    };
    complex j = complex {
            env->GetDoubleField(data, jxField),
            env->GetDoubleField(data, jyField)
    };
    complex p1 = (complex) {
            env->GetDoubleField(data, p1xField),
            env->GetDoubleField(data, p1yField)
    };
    complex p2 = (complex) {
            env->GetDoubleField(data, p2xField),
            env->GetDoubleField(data, p2yField)
    };
    complex p3 = (complex) {
            env->GetDoubleField(data, p3xField),
            env->GetDoubleField(data, p3yField)
    };
    complex p4 = (complex) {
            env->GetDoubleField(data, p4xField),
            env->GetDoubleField(data, p4yField)
    };


    float* imArray = new float[width*(yEnd - yStart)*2];
//    jobject myObj = env->NewGlobalRef(jobj);
//    jclass clazz = env->FindClass("com/selfsimilartech/fractaleye/FractalRenderer");
//    jmethodID messageMe = env->GetMethodID(clazz, "incrementProgressBar", "()V");
//    JavaVM *vm = 0;
//    env->GetJavaVM(&vm);


# pragma omp parallel for
    for (int y = yStart; y < yEnd; y++) {

//        if (y % (height/50) == 0) {
//            JNIEnv *myEnv;
//            vm->GetEnv((void **)&myEnv, JNI_VERSION_1_6);
//            vm->AttachCurrentThread(&myEnv, NULL);
//            myEnv->CallVoidMethod(myObj, messageMe);
//        }

        //LOGD("rendering row %d", y);
        for (int x = 0; x < width; x++) {
            //LOGD("rendering col %d", x);

            // [-1.0, 1.0]
            double u = (2.0 * (x / double(width)) - 1.0)*bgScale;
            double v = (2.0 * (y / double(height)) - 1.0)*bgScale;

            double aAux = u * scale;
            double bAux = v * scale * aspectRatio;
            double a = aAux * cosRotation - bAux * sinRotation + xCoord;
            double b = aAux * sinRotation + bAux * cosRotation + yCoord;
            complex c = complex { a, b };

            complex z;
            complex z1;
            if (juliaMode){
                z1 = c;
                c = j;
            } else {
                z1 = z0;
            }
            double zModSqr;
            //double smoothSum = 0.0;

            for (int n = 0; n <= maxIter; n++) {

                // shape loop
                //z = csin(cadd(cdiv(p1, z1), cdiv(z1, c)));
                z = csin(cdiv(cadd(cmult(p1, c), csqr(z1)), cmult(z1, c)));

                zModSqr = cabs2(z);
                //smoothSum += exp(-sqrt(zModSqr));

                if (zModSqr > escapeRadius * escapeRadius) {
                    //imArray[2 * ((y - yStart) * width + x)] = float(n)/float(maxIter);
                    imArray[2 * ((y - yStart) * width + x)] = float(log(log(float(n) + 1.0) + 1.0));
                    imArray[2 * ((y - yStart) * width + x) + 1] = 0.f;
                    break;
                } else if (n == maxIter) {
                    imArray[2 * ((y - yStart) * width + x)] = 1.f;
                    imArray[2 * ((y - yStart) * width + x) + 1] = 1.f;
                }

                z1 = z;

            }

        }
    }


    //env->DeleteGlobalRef(jobj);
    jfloatArray imJavaArray = env->NewFloatArray(width*(yEnd - yStart)*2);
    env->SetFloatArrayRegion(imJavaArray, 0, width*(yEnd - yStart)*2, imArray);
    return imJavaArray;


}
#pragma clang diagnostic pop
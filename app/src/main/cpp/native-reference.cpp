#include <jni.h>
#include <string>
#include <stdio.h>
#include <android/log.h>
#include <gmpxx.h>

#pragma clang diagnostic push
#pragma ide diagnostic ignored "err_typecheck_invalid_operands"
#define APPNAME "app jni"
#define LOGD(FORMAT, VALUE) __android_log_print(ANDROID_LOG_DEBUG , APPNAME, FORMAT, VALUE);

extern "C" JNIEXPORT jdoubleArray

JNICALL
Java_com_selfsimilartech_fractaleye_FractalRenderer_iterateReferenceNative(
        JNIEnv *env,
        jobject /* this */,
        jstring z0xIn,
        jstring z0yIn,
        jdoubleArray d0xIn,
        jdoubleArray d0yIn,
        jint precision,
        jint maxIter,
        jint maxRefIter,
        jdouble escapeRadius,
        jdouble sp,
        jdouble sn,
        jobject data) {


    mpf_set_default_prec((mp_bitcnt_t)(precision*4));  // BITS NOT DIGITS

    // main variables
    int refIter = 0;
    jdoubleArray refJavaArray = env->NewDoubleArray(2*maxRefIter);
    double refArray[2*maxRefIter];
    const char *z0xPtr = env->GetStringUTFChars(z0xIn, 0);
    const char *z0yPtr = env->GetStringUTFChars(z0yIn, 0);
    mpf_class z0x(z0xPtr); mpf_class z0y(z0yPtr);
    mpf_class z1x = z0x; mpf_class z1y = z0y;
    mpf_class zx; mpf_class zy;
    mpf_class z1xSqr; mpf_class z1ySqr;
    mpf_class modSqr;
    double z1xd; double z1yd;


    // derivatives
    double a1x = 1.0; double a1y = 0.0;
    double b1x = 0.0; double b1y = 0.0;
    double c1x = 0.0; double c1y = 0.0;
    double ax; double ay;
    double bx; double by;
    double cx; double cy;


    // approximation
    int skipIter = -1;
    jboolean isCopy;
    double *d0x = env->GetDoubleArrayElements(d0xIn, &isCopy);
    double *d0y = env->GetDoubleArrayElements(d0yIn, &isCopy);
    //env->ReleaseDoubleArrayElements(d0xIn, d0x, 0);
    //env->ReleaseDoubleArrayElements(d0yIn, d0y, 0);
    double d1x[8] { 0.0 }; double d1y[8] { 0.0 };
    double dx[8]  { 0.0 }; double dy[8]  { 0.0 };
    double d0xSqr[8];   double d0ySqr[8];
    double d0xCube[8];  double d0yCube[8];
    if (abs(d0x[0]) < 1e-100) {
        // start scaling initial deltas and series coefficients to avoid infinities
        // d0 remains unshifted, d0Sqr and d0Cube are shifted up
        // a remains unshifted, b and c are shifted down
        a1x *= sn;
        for (int i = 0; i < 8; i++) {
            d0x[i] *= sp;
            d0y[i] *= sp;
            d0xSqr[i] = (d0x[i]*sn)*d0x[i] - (d0y[i]*sn)*d0y[i];
            d0ySqr[i] = 2.0*(d0x[i]*sn)*d0y[i];
            d0xCube[i] = (d0x[i]*sn)*d0xSqr[i] - (d0y[i]*sn)*d0ySqr[i];
            d0yCube[i] = (d0x[i]*sn)*d0ySqr[i] + (d0y[i]*sn)*d0xSqr[i];
//            LOGD("d0x: %e", d0x[i])
//            LOGD("d0y: %e", d0y[i])
//            LOGD("d0xSqr: %e", d0xSqr[i])
//            LOGD("d0ySqr: %e", d0ySqr[i])
//            LOGD("d0xCube: %e", d0xCube[i])
//            LOGD("d0yCube: %e", d0yCube[i])
        }
    }
    else {
        for (int i = 0; i < 8; i++) {
            d0xSqr[i] = d0x[i]*d0x[i] - d0y[i]*d0y[i];
            d0ySqr[i] = 2.0*d0x[i]*d0y[i];
            d0xCube[i] = d0x[i]*d0xSqr[i] - d0y[i]*d0ySqr[i];
            d0yCube[i] = d0x[i]*d0ySqr[i] + d0y[i]*d0xSqr[i];
        }
    }
    double dxa; double dya;
    double error1;
    double error2;

    for (int i = 0; (skipIter == -1 || i < skipIter + maxRefIter) && i <= maxIter; i++) {

        z1xSqr = z1x*z1x;
        z1ySqr = z1y*z1y;
        zx = z1xSqr - z1ySqr + z0x;
        zy = 2.0*z1x*z1y + z0y;

        modSqr = z1xSqr + z1ySqr;

        z1xd = z1x.get_d(); z1yd = z1y.get_d();

        if (skipIter == -1) {

            // iterate derivatives
            // a = 2*z1*a1 + 1.0
            ax = 2.0*(z1xd*a1x - z1yd*a1y) + 1.0*sn;                        ay = 2.0*(z1xd*a1y + z1yd*a1x);
            // b = 2*z1*b1 + a1^2
            bx = 2.0*(z1xd*b1x - z1yd*b1y) + (a1x*sp)*a1x - (a1y*sp)*a1y;   by = 2.0*(z1xd*b1y + z1yd*b1x + (a1x*sp)*a1y);
            // c = 2*(z1*c1 + a1*b1)
            cx = 2.0*(z1xd*c1x - z1yd*c1y + (a1x*sp)*b1x - (a1y*sp)*b1y);   cy = 2.0*(z1xd*c1y + z1yd*c1x + (a1x*sp)*b1y + (a1y*sp)*b1x);

            // iterate probe points --- d = 2*z1*d1 + d1^2 + d0
            for (int j = 0; j < 8; j++) {

                dx[j] = 2.0*(z1xd*d1x[j] - z1yd*d1y[j]) + d1x[j]*d1x[j] - d1y[j]*d1y[j] + d0x[j]*sn;
                dy[j] = 2.0*(z1xd*d1y[j] + z1yd*d1x[j] + d1x[j]*d1y[j]) + d0y[j]*sn;


                // construct approximation --- da = a*d0 + b*d0^2 + c*d0^3
                // shifted arithmetic should cancel out here
                double t1x = ax*d0x[j] - ay*d0y[j];
                double t1y = ax*d0y[j] + ay*d0x[j];
                double t2x = bx*d0xSqr[j] - by*d0ySqr[j];
                double t2y = bx*d0ySqr[j] + by*d0xSqr[j];
                double t3x = cx*d0xCube[j] - cy*d0yCube[j];
                double t3y = cx*d0yCube[j] + cy*d0xCube[j];
                dxa = t1x + t2x + t3x;
                dya = t1y + t2y + t3y;

                double xerror = dx[j] - dxa;
                double yerror = dy[j] - dya;

                error1 = (t3x*t3x + t3y*t3y)/(t2x*t2x + t2y*t2y);
                error2 = xerror*xerror + yerror*yerror;
                if (error1 > 1e-3 || error2 > 1e-12) {
                    skipIter = i;
//                    LOGD("t1x: %e", t1x);
//                    LOGD("t1y: %e", t1y);
//                    LOGD("t2x: %e", t2x);
//                    LOGD("t2y: %e", t2y);
//                    LOGD("t3x: %e", t3x);
//                    LOGD("t3y: %e", t3y);
                }

            }

        }
        if (skipIter != -1) {  // series approximation no longer accurate

            refArray[2*(i - skipIter)]     = z1xd;
            refArray[2*(i - skipIter) + 1] = z1yd;

        }
        else {

            a1x = ax; a1y = ay;
            b1x = bx; b1y = by;
            c1x = cx; c1y = cy;

        }


        if (i == maxIter || i == skipIter + maxRefIter - 1) {
            refIter = i;
        }
        else if (modSqr > escapeRadius*escapeRadius) {
            refIter = i;
            break;
        }

        z1x = zx;
        z1y = zy;
        for (int j = 0; j < 8; j++) {
            d1x[j] = dx[j];
            d1y[j] = dy[j];
        }

    }

    // Get the class of the input object
    jclass dataClass = env->GetObjectClass(data);

    // Get Field references
    jfieldID refIterField   = env->GetFieldID(dataClass, "refIter",  "I");
    jfieldID skipIterField  = env->GetFieldID(dataClass, "skipIter", "I");
    jfieldID axField        = env->GetFieldID(dataClass, "ax",       "D");
    jfieldID ayField        = env->GetFieldID(dataClass, "ay",       "D");
    jfieldID bxField        = env->GetFieldID(dataClass, "bx",       "D");
    jfieldID byField        = env->GetFieldID(dataClass, "by",       "D");
    jfieldID cxField        = env->GetFieldID(dataClass, "cx",       "D");
    jfieldID cyField        = env->GetFieldID(dataClass, "cy",       "D");

    // Set fields for object
    env->SetIntField(data, refIterField, refIter);
    env->SetIntField(data, skipIterField, skipIter);
    env->SetDoubleField(data, axField, a1x);  env->SetDoubleField(data, ayField, a1y);
    env->SetDoubleField(data, bxField, b1x);  env->SetDoubleField(data, byField, b1y);
    env->SetDoubleField(data, cxField, c1x);  env->SetDoubleField(data, cyField, c1y);

    env->SetDoubleArrayRegion(refJavaArray, 0, maxRefIter*2, refArray);
    return refJavaArray;


}
#pragma clang diagnostic pop
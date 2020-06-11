#pragma version(1)
#pragma rs java_package_name(com.selfsimilartech.fractaleye)
#pragma rs_fp_full
#include <rs_fractal.rsh>


IterateData_t data;

float2 RS_KERNEL iterate(uint32_t x, uint32_t y) {

    double width = data.width;
    double height = data.height;
    double aspectRatio = data.aspectRatio;
    double bgScale = data.bgScale;

    uint32_t maxIter = data.maxIter;
    float escapeRadius = data.escapeRadius;

    bool juliaMode = data.juliaMode;
    complex j = (complex) { data.jx, data.jy };
    complex p1 = (complex) { data.p1x, data.p1y };
    complex p2 = (complex) { data.p2x, data.p2y };
    complex p3 = (complex) { data.p3x, data.p3y };
    complex p4 = (complex) { data.p4x, data.p4y };

    double scale = data.scale;
    double xCoord = data.xCoord;
    double yCoord = data.yCoord;
    double sinRotation = data.sinRotation;
    double cosRotation = data.cosRotation;

    // send update progress message at regular intervals
    if (x == 0 && y % (int)(height/50) == 0) { rsSendToClient(0); }

    float2 color;

    // [-1.0, 1.0]
    double u = (2.0*(x/width) - 1.0)*bgScale;
    double v = (2.0*(y/height) - 1.0)*bgScale;

    double aAux = u*scale;
    double bAux = v*scale*aspectRatio;
    double a = aAux*cosRotation - bAux*sinRotation + xCoord;
    double b = aAux*sinRotation + bAux*cosRotation + yCoord;
    complex c = { a, b };

    complex z = { 0.0, 0.0 };
    complex z1 = { 0.0, 0.0 };
    if (juliaMode) {
        z1 = c;
        c = j;
    }
    double zModSqr = 0.0;
    float smoothSum = 0.0;

    for (int n = 0; n <= maxIter; n++) {


        // $ SHAPE LOOP $
        z = cadd(cquint(z1), c);

        zModSqr = cabs2(z);
        smoothSum += exp(-sqrt((float)zModSqr));

        if (zModSqr > escapeRadius*escapeRadius) {
            //color = (float2) { (float)n / (float)maxIter, 0.f };
            color = (float2) { log(log(smoothSum + 1.f) + 1.f), 0.f };
            break;
        }
        else if (n == maxIter) {
            color = (float2) { 1.f, 1.f };
        }


        z1 = z;

    }


    return color;


}

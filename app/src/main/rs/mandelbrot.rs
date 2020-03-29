#pragma version(1)
#pragma rs java_package_name(com.selfsimilartech.fractaleye)
#pragma rs_fp_full


rs_allocation ref;

double width;
double height;
double aspectRatio;

uint32_t maxIter;
double scale;
double xCoord;
double yCoord;
double sinRotation;
double cosRotation;





uchar4 RS_KERNEL iterate(uint32_t x, uint32_t y) {

    // send update progress message at regular intervals
    if (x == 0 && y % (int)(height/50) == 0) { rsSendToClient(0); }

    uchar4 color;

    // [-1.0, 1.0]
    double u = 2.0*(x/width) - 1.0;
    double v = 2.0*(y/height) - 1.0;

    double aAux = u*scale;
    double bAux = v*scale*aspectRatio;
    double a = aAux*cosRotation - bAux*sinRotation + xCoord;
    double b = aAux*sinRotation + bAux*cosRotation + yCoord;

    double zx = 0.0;
    double zy = 0.0;
    double zx_temp = 0.0;


    for (int n = 0; n <= maxIter; n++) {

        if (n == maxIter) {
            color = (uchar4){255, 255, 0, 255};
        }

        zx_temp = zx*zx - zy*zy + a;
        zy = 2.0*zx*zy + b;
        zx = zx_temp;

        if (zx*zx + zy*zy > 4.0) {
            color = (uchar4){(uchar)(255*n/(float)(maxIter)), 0, 0, 255};
            break;
        }

    }


    return color;


}

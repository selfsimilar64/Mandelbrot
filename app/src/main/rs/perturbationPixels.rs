#pragma version(1)
#pragma rs java_package_name(com.selfsimilartech.fractaleye)
#pragma rs_fp_full


rs_allocation ref;
rs_allocation pixels;

double width;
double height;
double pixelsSize;
double aspectRatio;
double bgScale;

uint32_t maxRefIter;
uint32_t refIter;
uint32_t skipIter;
uint32_t maxIter;
float escapeRadius;

double d0xOffset;
double d0yOffset;

double scale;
double sinRotation;
double cosRotation;

double alphax;
double alphay;
double betax;
double betay;
double gammax;
double gammay;

double sp;
double sn;





float2 RS_KERNEL iterate(uint32_t x) {

    short2 pixel = rsGetElementAt_short2(pixels, x);
    float2 color;

    // send update progress message at regular intervals
    //if (x % (int)(pixelsSize/50) == 0) { rsSendToClient(0); }


    // [-1.0, 1.0]
    double u = bgScale*(2.0*(pixel.x/width) - 1.0);
    double v = bgScale*(2.0*(pixel.y/height) - 1.0);

    double d0xAux = u*scale;
    double d0yAux = v*scale*aspectRatio;

    double d0x = d0xAux*cosRotation - d0yAux*sinRotation + d0xOffset;
    double d0y = d0xAux*sinRotation + d0yAux*cosRotation + d0yOffset;

    double d0sqrx = (d0x*sp)*d0x - (d0y*sp)*d0y;
    double d0sqry = 2.0*(d0x*sp)*d0y;

    double d0cubx = d0x*d0sqrx - d0y*d0sqry;
    double d0cuby = d0x*d0sqry + d0y*d0sqrx;

    double dx;
    double dy;
    double t1x = (alphax*sp)*d0x - (alphay*sp)*d0y;
    double t1y = (alphax*sp)*d0y + (alphay*sp)*d0x;
    double t2x = betax*d0sqrx - betay*d0sqry;
    double t2y = betax*d0sqry + betay*d0sqrx;
    double t3x = gammax*d0cubx - gammay*d0cuby;
    double t3y = gammax*d0cuby + gammay*d0cubx;
    double d1x = t1x + t2x + t3x;
    double d1y = t1y + t2y + t3y;


    double zx;
    double zy;
    double z1x = rsGetElementAt_double(ref, 0);
    double z1y = rsGetElementAt_double(ref, 1);

    double wx;
    double wy;
    double wModSqr;


    for (int i = skipIter; i < refIter; i++) {

        zx = rsGetElementAt_double(ref, 2*((i - skipIter)+1));
        zy = rsGetElementAt_double(ref, 2*((i - skipIter)+1) + 1);

        dx = 2.0*(z1x*d1x - z1y*d1y) + d1x*d1x - d1y*d1y + d0x;
        dy = 2.0*(z1x*d1y + z1y*d1x + d1x*d1y) + d0y;

        wx = zx + dx;
        wy = zy + dy;

        wModSqr = wx*wx + wy*wy;


        if (wModSqr > escapeRadius*escapeRadius) {
            float smooth = (float)i - log(0.25f*log((float)(wModSqr)))/log(2.f);
            //color = (float2) { log10((float)i/(float)maxIter*(10.f - 1.f) + 1.f), 0.f };
            color = (float2) { (float)i/(float)maxIter, 0.f };
            break;
        }
        else if (wModSqr/(zx*zx + zy*zy) < 1e-6) {
            color = (float2) { 1.f, 3.f };
            break;
        }
        else if (i == refIter - 1) {
            if (refIter == maxIter || refIter - skipIter == maxRefIter - 1) {
                color = (float2) { 1.f, 1.f };
            }
            else {
                color = (float2) { 1.f, 3.f };
            }
        }


        z1x = zx;
        z1y = zy;

        d1x = dx;
        d1y = dy;


    }


    return color;


}

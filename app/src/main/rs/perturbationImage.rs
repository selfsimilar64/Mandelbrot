#pragma version(1)
#pragma rs java_package_name(com.selfsimilartech.fractaleye)
#pragma rs_fp_full


rs_allocation ref;

double width;
double height;
double aspectRatio;
double bgScale;

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





float2 RS_KERNEL iterate(uint32_t x, uint32_t y) {

    float2 color;

    // send update progress message at regular intervals
    if (y % (int)(height/50) == 0) { rsSendToClient(0); }


    // [-1.0, 1.0]
    double u = bgScale*(2.0*(x/(width - 1.0)) - 1.0);
    double v = bgScale*(2.0*(y/(height - 1.0)) - 1.0);

    double d0xAux = u*scale;
    double d0yAux = v*scale*aspectRatio;

    double d0x = d0xAux*cosRotation - d0yAux*sinRotation + d0xOffset;
    double d0y = d0xAux*sinRotation + d0yAux*cosRotation + d0yOffset;

    double d0sqrx = d0x*d0x - d0y*d0y;
    double d0sqry = 2.0*d0x*d0y;

    double d0cubx = d0x*d0sqrx - d0y*d0sqry;
    double d0cuby = d0x*d0sqry + d0y*d0sqrx;

    double dx;
    double dy;
    double d1x = alphax*d0x - alphay*d0y + betax*d0sqrx - betay*d0sqry + gammax*d0cubx - gammay*d0cuby;
    double d1y = alphax*d0y + alphay*d0x + betax*d0sqry + betay*d0sqrx + gammax*d0cuby + gammay*d0cubx;


    double zx;
    double zy;
    double z1x = rsGetElementAt_double(ref, 2*skipIter);
    double z1y = rsGetElementAt_double(ref, 2*skipIter + 1);

    double wx;
    double wy;
    double wModSqr;


    for (int i = skipIter; i < refIter; i++) {

        zx = rsGetElementAt_double(ref, 2*(i+1));
        zy = rsGetElementAt_double(ref, 2*(i+1) + 1);

        dx = 2.0*(z1x*d1x - z1y*d1y) + d1x*d1x - d1y*d1y + d0x;
        dy = 2.0*(z1x*d1y + z1y*d1x + d1x*d1y) + d0y;

        wx = zx + dx;
        wy = zy + dy;

        wModSqr = wx*wx + wy*wy;

        if (wModSqr > escapeRadius*escapeRadius) {
            float smooth = (float)i - log(0.25f*log((float)(wModSqr)))/log(2.f);
            // color = (uchar4){ (uchar)((255.f*((float)(i) - smooth)/(float)(maxIter))), 0, 0, 255 };
            color = (float2) { smooth/(float)maxIter, 0.f };
            break;
        }
        else if (wModSqr/(zx*zx + zy*zy) < 0.000001) {
            color = (float2) { 1.f, 3.f };
        }
        else if (i == refIter - 1) {
            if (refIter == maxIter) {
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

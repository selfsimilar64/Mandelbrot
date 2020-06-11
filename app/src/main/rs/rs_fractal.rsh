#ifndef RENDERSCRIPT_RS_FRACTAL_RSH
#define RENDERSCRIPT_RS_FRACTAL_RSH

typedef struct IterateData {

    int width;
    int height;
    int yStart;
    int yEnd;
    double aspectRatio;
    double bgScale;

    uint32_t maxIter;
    float escapeRadius;

    double scale;
    double xCoord;
    double yCoord;
    double sinRotation;
    double cosRotation;

    bool juliaMode;
    double jx;
    double jy;

    double x0;
    double y0;
    double p1x;
    double p1y;
    double p2x;
    double p2y;
    double p3x;
    double p3y;
    double p4x;
    double p4y;

} IterateData_t;






typedef struct { double x, y; } complex;


static const double pi = (double)M_PI;
static const complex PI = (complex) { pi, 0.0 };
static const complex I = (complex) { 0.0, 1.0 };
static const complex ONE = (complex) { 1.0, 0.0 };



static inline double absd(double a) {  // absolute value of double
    double b = a;
    if (b < 0.0) b *= -1.0;
    return b;
}


static inline complex cconj(complex z) {
    return (complex) { z.x, -z.y };
}

static inline complex cneg(complex z) {
    return (complex) { -z.x, -z.y };
}

static inline complex cnegr(complex z) {
    return (complex) { -z.x, z.y };
}

static inline complex cmult(complex z, complex w) {
    return (complex) { z.x*w.x - z.y*w.y, z.x*w.y + z.y*w.x };
}

static inline complex cmultd(double a, complex z) {
    return (complex) { a*z.x, a*z.y };
}

static inline complex cdivd(complex z, double a) {
    return (complex) { z.x/a, z.y/a };
}

static inline complex csqr(complex z) {
    return (complex) {
        z.x*z.x - z.y*z.y,
        2.0*z.x*z.y
    };
}

static inline complex ccube(complex z) {
    double xSqr = z.x*z.x;
    double ySqr = z.y*z.y;
    return (complex) {
        z.x*(xSqr - 3.0*ySqr),
        z.y*(3.0*xSqr - ySqr)
    };
}

static inline complex cquad(complex z) {
    double xSqr = z.x*z.x;
    double ySqr = z.y*z.y;
    return (complex) {
        xSqr*(xSqr - 3.0*ySqr) - ySqr*(3.0*xSqr - ySqr),
        4.0*z.x*z.y*(xSqr - ySqr)
    };
}

static inline complex cquint(complex z) {
    double xSqr = z.x*z.x;
    double ySqr = z.y*z.y;
    return (complex) {
        z.x*xSqr*(xSqr - 3.0*ySqr) - z.x*ySqr*(7.0*xSqr - 5.0*ySqr),
        xSqr*z.y*(5.0*xSqr - 7.0*ySqr) - z.y*ySqr*(3.0*xSqr - ySqr)
    };
}

static inline complex cadd(complex z, complex w) {
    return (complex) { z.x + w.x, z.y + w.y };
}

static inline complex csub(complex z, complex w) {
    return (complex) { z.x - w.x, z.y - w.y };
}

static inline double cabs2(complex z) {
    return z.x*z.x + z.y*z.y;
}

static inline double dmod(double a) { // a % 1.0
    float b = floor((float)a);
    return a - (double)b;
}

static inline complex cinv(complex z) {
    complex w = cconj(z);
    double modSqr = cabs2(z);
    return (complex) {
        w.x/modSqr,
        w.y/modSqr
    };
}

static inline complex cdiv(complex z, complex w) {
    double wModSqr = cabs2(w);
    complex num = cmult(z, cconj(w));
    return (complex) {
        num.x / wModSqr,
        num.y / wModSqr
    };
}

static inline complex bhaskara_sin(complex z) {
    complex w = cmultd(4.0, cmult(z, csub(PI, z)));
    double wModSqr = cabs2(w);
    double q = 5.0*pi*pi;
    double denom = q*(q - 2.0*w.x) + wModSqr;
    if (denom == 0.0) return (complex) { 0.0, 0.0 };
    return (complex) {
        4.0*(q*w.x - wModSqr) / denom,
        4.0*q*w.y / denom
    };
}

static inline complex csin_series1(complex z) {
    complex zModulo = (complex) { 2.0*pi*dmod(0.5*(z.x/pi - 1.0)) - pi, z.y };
    complex zSqr = csqr(zModulo);
    complex zPow = zModulo;
    complex t = zModulo;
    double a = 1.0;
    double s = 1.0;
    for (double i = 3.0; i <= 11.0; i += 2.0) {
        s *= -1.0;
        a *= i*(i - 1.0);
        zPow = cmult(zPow, zSqr);
        t = cadd(t, cmultd(s/a, zPow));
    }
    return t;
}

static inline complex csin_series2(complex z) {  // expansion around pi/2
    complex zModulo = (complex) { 2.0*pi*dmod(0.5*(z.x/pi - 1.0)) - pi - pi/2.0, z.y };
    complex zSqr = csqr(zModulo);
    complex zPow = ONE;
    complex t = ONE;
    double a = 1.0;
    double s = 1.0;
    for (double i = 2.0; i <= 10.0; i += 2.0) {
        s *= -1.0;
        a *= i*(i - 1.0);
        zPow = cmult(zPow, zSqr);
        t = cadd(t, cmultd(s/a, zPow));
    }
    return t;
}

static inline complex csin(complex z) {
    complex w = (complex) { 2.0*pi*dmod(0.5*(z.x/pi - 1.0)) - pi, z.y };
    if (w.x < -3.0*pi/4.0) { return csin_series1(csub(cneg(PI), w)); }
    else if (w.x >= -3.0*pi/4.0 && w.x <= -pi/4.0) { return cneg(csin_series2(cneg(w))); }
    else if (w.x > -pi/4.0 && w.x < pi/4.0) { return csin_series1(w); }
    else if (w.x >= pi/4.0 && w.x <= 3.0*pi/4.0) { return csin_series2(w); }
    else if (w.x > 3.0*pi/4.0) { return csin_series1(csub(PI, w)); }
}

static inline complex cboxfold(complex z) {
    complex w = z;
    if (z.x < -1.0) { w.x = -2.0 - z.x; }
    else if (z.x > 1.0) { w.x = 2.0 - z.x; }
    if (z.y < -1.0) { w.y = -2.0 - z.y; }
    else if (z.y > 1.0) { w.y = 2.0 - z.y; }
    return w;
}

static inline complex cballfold(complex z) {
    double zModSqr = cabs2(z);
    double c = 1.0;
    if (zModSqr < 0.25) { c = 4.0; }
    else if (zModSqr >= 0.25 && zModSqr < 1.0) { c = 1.0/zModSqr; }
    return cmultd(c, z);
}

static inline complex cabse(complex z) {  // element-wise absolute value
    return (complex) { absd(z.x), absd(z.y) };
}

static inline complex kali(complex z) {
    complex w = cabse(z);
    return cdivd(w, cabs2(w));
}



#endif
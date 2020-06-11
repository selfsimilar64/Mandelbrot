//
// Created by mattj on 5/25/2020.
//

#ifndef MANDELBROT_COMPLEXMATH_H
#define MANDELBROT_COMPLEXMATH_H



typedef struct {
    double x;
    double y;
} complex;

const complex i = complex { 0.0, 1.0 };
const complex one = complex { 1.0, 0.0 };



static inline complex cconj(complex z) {
    return (complex) { z.x, -z.y };
}

static inline complex cneg(complex z) {
    return (complex) { -z.x, -z.y };
}

static inline complex cmult(complex z, complex w) {
    return (complex) {
            z.x*w.x - z.y*w.y,
            z.x*w.y + z.y*w.x
    };
}

static inline complex cmult(double a, complex z) {
    return complex {
        a*z.x,
        a*z.y
    };
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
    return (complex) {
            z.x + w.x,
            z.y + w.y
    };
}

static inline complex cadd(complex z, double a) {
    return complex {
        z.x + a,
        z.y
    };
}

static inline complex csub(complex z, complex w) {
    return (complex) {
            z.x - w.x,
            z.y - w.y
    };
}

static inline double cabs(complex z) {
    return sqrt(z.x*z.x + z.y*z.y);
}

static inline double cabs2(complex z) {
    return z.x*z.x + z.y*z.y;
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

static inline complex cdiv(complex z, double a) {
    return complex {
        z.x/a,
        z.y/a
    };
}

static inline complex csin(complex z) {
    double t2 = exp(z.y);
    double t1 = 1.0/t2;
    double sinx;
    double cosx;
    sincos(z.x, &sinx, &cosx);
    return complex {
            0.5*sinx*(t1 + t2),
            -0.5*cosx*(t1 - t2)
    };
}

static inline complex ccos(complex z) {
    double t1 = exp(z.y);
    double t2 = 1.0/t1;
    double sinx;
    double cosx;
    sincos(z.x, &sinx, &cosx);
    return complex {
            0.5*cosx*(t1 + t2),
            -0.5*sinx*(t1 - t2)
    };
}

static inline complex ctan(complex z) {
    return cdiv(csin(z), ccos(z));
}

static inline complex cexp(complex z) {
    double a = exp(z.x);
    double siny;
    double cosy;
    sincos(z.y, &siny, &cosy);
    return complex {
            a*cosy,
            a*siny
    };
}

static inline complex cexpi(double x) {
    double sinx;
    double cosx;
    sincos(x, &sinx, &cosx);
    return complex { cosx, sinx };
}

static inline complex clog(complex z) {
    return complex { 0.5*log(cabs2(z) + 1e-16), atan2(z.y, z.x) };
}

static inline complex cpow(complex z, complex w) {
    return cexp(cmult(w, clog(z)));
}

#endif //MANDELBROT_COMPLEXMATH_H

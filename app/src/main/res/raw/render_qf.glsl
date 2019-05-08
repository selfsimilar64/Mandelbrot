#version 320 es
#define SPLIT 8193.
#define R 1e5
#define Sp 1e34
#define Sn 1e-34
#define Sh 1e-17

precision highp float;
uniform int maxIter;
uniform float xTouchPos;
uniform float yTouchPos;
uniform vec4 xScale;
uniform vec4 yScale;
uniform vec4 xOffset;
uniform vec4 yOffset;

in vec4 viewPos;
out vec4 fragmentColor;




vec2 quickTwoSum(float a, float b) {
    float s = a + b;
    float e = b - (s - a);
    return vec2(s, e);
}

vec2 twoSum(float a, float b) {
    float s = a + b;
    float v = s - a;
    float e = (a - (s - v)) + (b - v);
    return vec2(s, e);
}

vec3 threeSum(float a, float b, float c) {

    vec2 tmp;
    vec3 res = vec3(0.0);
    float t1, t2, t3;
    tmp = twoSum(a, b);
    t1 = tmp.x;
    t2 = tmp.y;

    tmp = twoSum(c, t1);
    res.x = tmp.x;
    t3 = tmp.y;

    tmp = twoSum(t2, t3);
    res.y = tmp.x;
    res.z = tmp.y;

    return res;

}

vec3 threeSumTwo(float a, float b, float c) {
    vec2 tmp;
    vec3 res = vec3(0.0);
    float t1, t2, t3;
    tmp = twoSum(a, b);
    t1 = tmp.x;
    t2 = tmp.y;

    tmp = twoSum(c, t1);
    res.x = tmp.x;
    t3 = tmp.y;

    res.y = t2 + t3;
    return res;
}

vec2 split(float a) {
    float t = a*SPLIT;
    float a_hi = t - (t - a);
    float a_lo = a - a_hi;
    return vec2(a_hi, a_lo);
}

vec2 twoProd(float a, float b) {
    float p = a*b;
    vec2 aS = split(a);
    vec2 bS = split(b);
    float err = ((aS.x*bS.x - p) + aS.x*bS.y + aS.y*bS.x) + aS.y*bS.y;
    return vec2(p, err);
}

vec2 twoSqr(float a) {
    float p = a*a;
    vec2 s = split(a);
    float e = ((s.x*s.x - p) + 2.0*s.x*s.y) + s.y*s.y;
    return vec2(p, e);
}




vec2 addDF(vec2 a, vec2 b) {
    vec2 s;
    vec2 t;
    s = twoSum(a.x, b.x);
    t = twoSum(a.y, b.y);
    s.y += t.x;
    s = quickTwoSum(s.x, s.y);
    s.y += t.y;
    s = quickTwoSum(s.x, s.y);
    return s;
}

vec2 multDF(vec2 a, vec2 b) {
    vec2 p;
    p = twoProd(a.x, b.x);
    p.y += a.x * b.y;
    p.y += a.y * b.x;
    p = quickTwoSum(p.x, p.y);
    return p;
}

vec2 divDF(vec2 a, vec2 b) {

    float xn = 1.0/b.x;
    float yn = a.x*xn;
    vec2 diff = addDF(a, -multDF(b, vec2(yn, 0.0)));
    vec2 prod = twoProd(xn, diff.x);
    return addDF(vec2(yn, 0.0), prod);

}

vec4 cMultDF(vec2 X, vec2 Y, vec2 A, vec2 B) {

//    vec2 k1 = multDF(A, addDF(X, Y));
//    vec2 k2 = multDF(X, addDF(B, -A));
//    vec2 k3 = multDF(Y, addDF(A, B));
//    vec2 U = addDF(k1, -k3);
//    vec2 V = addDF(k1, k2);

    vec2 U = addDF(multDF(X, A), -multDF(Y, B));
    vec2 V = addDF(multDF(Y, A), multDF(X, B));

    return vec4(U, V);

}

vec2 sqrDF(vec2 a) {
    vec2 p;
    p = twoSqr(a.x);
    p.y += 2.0*a.x*a.y;
    p = quickTwoSum(p.x, p.y);
    return p;
}

vec2 sqrtDF(vec2 a) {

    float xn = 1.0/sqrt(a.x);
    float yn = a.x*xn;
    vec2 ynSqr = sqrDF(vec2(yn, 0.0));

    float diff = (addDF(a, -ynSqr)).x;
    vec2 prod = twoProd(xn, diff)/2.0;

    return addDF(vec2(yn, 0.0), prod);

}

vec2 expDF(vec2 a) {

    float thresh = 1.0e-20*exp(a.x);
    vec2 t, p, f, s, x;
    float m;

    s = addDF(vec2(1.0, 0.0), a);
    p = sqrDF(a);
    m = 2.0;
    f = vec2(2.0, 0.0);
    t = p/2.0;
    while (abs(t.x) > thresh) {
        s = addDF(s, t);
        p = multDF(p, a);
        m += 1.0;
        f = multDF(f, vec2(m, 0.0));
        t = divDF(p, f); // switch??
    }

    return addDF(s, t);

}

vec2 logDF(vec2 a) {

    vec2 xi = vec2(0.);

    if (!(a.x == 1.0 && a.y == 0.0)) {
        if (a.x <= 0.0) {
            xi = vec2(log(a.x));
        }
        else {
            xi.x = log(a.x);
            xi = addDF(addDF(xi, multDF(expDF(-xi), a)), vec2(-1.0, 0.0));
        }
    }
    return xi;

}

vec2 modSqrDF(vec2 a, vec2 b) {
    return addDF(sqrDF(a), sqrDF(b));
}

vec2 modDF(vec2 a, vec2 b) {
    return sqrtDF(modSqrDF(a, b));
}

vec4 cDivDF(vec2 X, vec2 Y, vec2 A, vec2 B) {

    vec4 T = cMultDF(X, Y, A, -B);
    vec2 MOD2 = modSqrDF(A, B);
    vec2 U = divDF(T.xy, MOD2);
    vec2 V = divDF(T.zw, MOD2);
//    vec2 U = multDF(vec2(1.0/MOD2.x, 0.0), T.xy);
//    vec2 V = multDF(vec2(1.0/MOD2.x, 0.0), T.zw);
    return vec4(U, V);

}




vec4 renorm(float c0, float c1, float c2, float c3, float c4) {

    float s0, s1, s2 = 0.0, s3 = 0.0;
    vec2 tmp;

    tmp = quickTwoSum(c3, c4);
    s0 = tmp.x;
    c4 = tmp.y;

    tmp = quickTwoSum(c2, s0);
    s0 = tmp.x;
    c3 = tmp.y;

    tmp = quickTwoSum(c1, s0);
    s0 = tmp.x;
    c2 = tmp.y;

    tmp = quickTwoSum(c0, s0);
    c0 = tmp.x;
    c1 = tmp.y;

    s0 = c0;
    s1 = c1;

    tmp = quickTwoSum(c0, c1);
    s0 = tmp.x;
    s1 = tmp.y;

    if (s1 != 0.0) {
        tmp = quickTwoSum(s1, c2);
        s1 = tmp.x;
        s2 = tmp.y;

        if (s2 != 0.0) {
            tmp = quickTwoSum(s2, c3);
            s2 = tmp.x;
            s3 = tmp.y;
            if (s3 != 0.0) { s3 += c4; }
            else { s2 += c4; }
        }
        else {
            tmp = quickTwoSum(s1, c3);
            s1 = tmp.x;
            s2 = tmp.y;
            if (s2 != 0.0) {
                tmp = quickTwoSum(s2, c4);
                s2 = tmp.x;
                s3 = tmp.y;
            }
            else {
                tmp = quickTwoSum(s1, c4);
                s1 = tmp.x;
                s2 = tmp.y;
            }
        }
    }
    else {
        tmp = quickTwoSum(s0, c2);
        s0 = tmp.x;
        s1 = tmp.y;
        if (s1 != 0.0) {
            tmp = quickTwoSum(s1, c3);
            s1 = tmp.x;
            s2 = tmp.y;
            if (s2 != 0.0) {
                tmp = quickTwoSum(s2, c4);
                s2 = tmp.x;
                s3 = tmp.y;
            }
            else {
                tmp = quickTwoSum(s1, c4);
                s1 = tmp.x;
                s2 = tmp.y;
            }
        }
        else {
            tmp = quickTwoSum(s0, c3);
            s0 = tmp.x;
            s1 = tmp.y;
            if (s1 != 0.0) {
                tmp = quickTwoSum(s1, c4);
                s1 = tmp.x;
                s2 = tmp.y;
            }
            else {
                tmp = quickTwoSum(s0, c4);
                s0 = tmp.x;
                s1 = tmp.y;
            }
        }
    }

    return vec4(s0, s1, s2, s3);

}

vec4 addQF(vec4 a, vec4 b) {
    float s0, s1, s2, s3;
    float t0, t1, t2, t3;

    float v0, v1, v2, v3;
    float u0, u1, u2, u3;
    float w0, w1, w2, w3;

    vec2 tmp;
    vec3 tmp3;

    s0 = a.x + b.x;
    s1 = a.y + b.y;
    s2 = a.z + b.z;
    s3 = a.w + b.w;  

    v0 = s0 - a.x;
    v1 = s1 - a.y;
    v2 = s2 - a.z;
    v3 = s3 - a.w;

    u0 = s0 - v0;
    u1 = s1 - v1;
    u2 = s2 - v2;
    u3 = s3 - v3;

    w0 = a.x - u0;
    w1 = a.y - u1;
    w2 = a.z - u2;
    w3 = a.w - u3; 

    u0 = b.x - v0;
    u1 = b.y - v1;
    u2 = b.z - v2;
    u3 = b.w - v3;

    t0 = w0 + u0;
    t1 = w1 + u1;
    t2 = w2 + u2;
    t3 = w3 + u3;

    tmp = twoSum(s1, t0);
    s1 = tmp.x;
    t0 = tmp.y;

    tmp3 = threeSum(s2, t0, t1);
    s2 = tmp3.x;
    t0 = tmp3.y;
    t1 = tmp3.z;

    tmp3 = threeSumTwo(s3, t0, t2);
    s3 = tmp3.x;
    t0 = tmp3.y;
    t2 = tmp3.z;

    t0 = t0 + t1 + t3;

    return renorm(s0, s1, s2, s3, t0);
}

vec4 multQF(vec4 a, float b) {

    float p0, p1, p2, p3, p4, p5;
    float q0, q1, q2, q3, q4, q5;
    float t0, t1;
    float s0, s1, s2;
    vec2 tmp;
    vec3 tmp3;

    tmp = twoProd(a.x, b);
    p0 = tmp.x;
    q0 = tmp.y;

    tmp = twoProd(a.x, 0.0);
    p1 = tmp.x; // p1 == 0
    q1 = tmp.y; // q1 == 0

    tmp = twoProd(a.y, b);
    p2 = tmp.x;
    q2 = tmp.y;

    tmp = twoProd(a.x, 0.0);
    p3 = tmp.x; // p3 == 0
    q3 = tmp.y; // q3 == 0

    tmp = twoProd(a.y, 0.0);
    p4 = tmp.x; // p4 == 0
    q4 = tmp.y; // q4 == 0

    tmp = twoProd(a.z, b);
    p5 = tmp.x;
    q5 = tmp.y;

    /* Start Accumulation */
    tmp3 = threeSum(p1, p2, q0);
    // tmp3 = twoSum(p2, q0);
    p1 = tmp3.x;
    p2 = tmp3.y;
    q0 = tmp3.z;

    /* Six-Three Sum  of p2, q1, q2, p3, p4, p5. */
    tmp3 = threeSum(p2, q1, q2);
    p2 = tmp3.x;
    q1 = tmp3.y;
    q2 = tmp3.z;

    tmp3 = threeSum(p3, p4, p5);
    p3 = tmp3.x;
    p4 = tmp3.y;
    p5 = tmp3.z;


    /* compute (s0, s1, s2) = (p2, q1, q2) + (p3, p4, p5). */
    tmp = twoSum(p2, p3);
    s0 = tmp.x;
    t0 = tmp.y;

    tmp = twoSum(q1, p4);
    s1 = tmp.x;
    t1 = tmp.y;

    s2 = q2 + p5;
    tmp = twoSum(s1, t0);
    s1 = tmp.x;
    t0 = tmp.y;
    s2 += (t0 + t1);

    /* O(eps^3) order terms */
    s1 += a.w*b + q0 + q3 + q4 + q5;

    return renorm(p0, p1, s0, s1, s2);
    
}

vec4 multQF(vec4 a, vec4 b) {
    
    float p0, p1, p2, p3, p4, p5;
    float q0, q1, q2, q3, q4, q5;
    float t0, t1;
    float s0, s1, s2;
    vec2 tmp;
    vec3 tmp3;

    tmp = twoProd(a.x, b.x);
    p0 = tmp.x;
    q0 = tmp.y;

    tmp = twoProd(a.x, b.y);
    p1 = tmp.x;
    q1 = tmp.y;

    tmp = twoProd(a.y, b.x);
    p2 = tmp.x;
    q2 = tmp.y;

    tmp = twoProd(a.x, b.z);
    p3 = tmp.x;
    q3 = tmp.y;

    tmp = twoProd(a.y, b.y);
    p4 = tmp.x;
    q4 = tmp.y;

    tmp = twoProd(a.z, b.x);
    p5 = tmp.x;
    q5 = tmp.y;

    /* Start Accumulation */
    tmp3 = threeSum(p1, p2, q0);
    p1 = tmp3.x;
    p2 = tmp3.y;
    q0 = tmp3.z;

    /* Six-Three Sum  of p2, q1, q2, p3, p4, p5. */
    tmp3 = threeSum(p2, q1, q2);
    p2 = tmp3.x;
    q1 = tmp3.y;
    q2 = tmp3.z;

    tmp3 = threeSum(p3, p4, p5);
    p3 = tmp3.x;
    p4 = tmp3.y;
    p5 = tmp3.z;


    /* compute (s0, s1, s2) = (p2, q1, q2) + (p3, p4, p5). */
    tmp = twoSum(p2, p3);
    s0 = tmp.x;
    t0 = tmp.y;

    tmp = twoSum(q1, p4);
    s1 = tmp.x;
    t1 = tmp.y;

    s2 = q2 + p5;
    tmp = twoSum(s1, t0);
    s1 = tmp.x;
    t0 = tmp.y;
    s2 += (t0 + t1);

    /* O(eps^3) order terms */
    s1 += a.x*b.w + a.y*b.z + a.z*b.y + a.w*b.x + q0 + q3 + q4 + q5;

    return renorm(p0, p1, s0, s1, s2);
    
}

vec4 sqrQF(vec4 a) {

    float p0, p1, p2, p3, p4, p5;
    float q0, q1, q2, q3, q4, q5;
    float t0, t1;
    float s0, s1, s2;
    vec2 tmp;
    vec3 tmp3;

    tmp = twoSqr(a.x);
    p0 = tmp.x;
    q0 = tmp.y;

    tmp = twoProd(a.x, a.y);
    p1 = tmp.x;
    q1 = tmp.y;

    p2 = tmp.x;
    q2 = tmp.y;

    tmp = twoSqr(a.y);
    p4 = tmp.x;
    q4 = tmp.y;

    tmp = twoProd(a.x, a.z);
    p3 = tmp.x;
    q3 = tmp.y;

    p5 = tmp.x;
    q5 = tmp.y;

    /* Start Accumulation */
    // p1 == p2 -- optimize?
    tmp3 = threeSum(p1, p2, q0);
    p1 = tmp3.x;
    p2 = tmp3.y;
    q0 = tmp3.z;

    /* Six-Three Sum  of p2, q1, q2, p3, p4, p5. */
    // q1 == q2 -- optimize?
    tmp3 = threeSum(p2, q1, q2);
    p2 = tmp3.x;
    q1 = tmp3.y;
    q2 = tmp3.z;

    // p3 == p5 -- optimize?
    tmp3 = threeSum(p3, p4, p5);
    p3 = tmp3.x;
    p4 = tmp3.y;
    p5 = tmp3.z;


    /* compute (s0, s1, s2) = (p2, q1, q2) + (p3, p4, p5). */
    tmp = twoSum(p2, p3);
    s0 = tmp.x;
    t0 = tmp.y;

    tmp = twoSum(q1, p4);
    s1 = tmp.x;
    t1 = tmp.y;

    s2 = q2 + p5;
    tmp = twoSum(s1, t0);
    s1 = tmp.x;
    t0 = tmp.y;
    s2 += (t0 + t1);

    /* O(eps^3) order terms */
    s1 += 2.0*a.x*a.w + 2.0*a.y*a.z + q0 + 2.0*q3 + q4;

    return renorm(p0, p1, s0, s1, s2);

}

vec4 modSqrQF(vec4 a, vec4 b) {
    return addQF(sqrQF(a), sqrQF(b));
}

vec4 absQF(vec4 a) {
    if (a.x < 0.0) { return -a; }
    else { return a; }
}

vec4 mandelbrot_x(vec4 X, vec4 Y, vec4 xC) {
    return addQF(addQF(sqrQF(X), -sqrQF(Y)), xC);
}

vec4 mandelbrot_y(vec4 X, vec4 Y, vec4 yC) {
    vec4 T = multQF(X, Y);
    return addQF(addQF(T, T), yC);
}




vec2 conjSF(vec2 p) {
    return vec2(p.x, -p.y);
}

float modSqrSF(vec2 p) {
    return p.x*p.x + p.y*p.y;
}

vec2 cMultSF(vec2 p, vec2 q) {
    return vec2(p.x*q.x - p.y*q.y, p.y*q.x + p.x*q.y);
}

vec2 cDivSF(vec2 p, vec2 q) {
    return cMultSF(p, conjSF(q)) / modSqrSF(q);
}

float modSF(vec2 p) {
    return sqrt(p.x*p.x + p.y*p.y);
}




float _m(float _a, float _b) {
    return _a*_b * Sp;
}

float _d(float _a, float _b) {
    return _a/_b * Sn;
}

float _sqrt(float _a) {
    return sqrt(_a) * Sh;
}

vec2 _twoSqr(float _a) {
    float _p = _m(_a, _a);
    vec2 _s = split(_a);
    float _e = ((_m(_s.x, _s.x) - _p) + 2.0*_m(_s.x, _s.y)) + _m(_s.y, _s.y);
    return vec2(_p, _e);
}

vec2 _twoProd(float _a, float _b) {
    float _p = _m(_a, _b);
    vec2 _aS = split(_a);
    vec2 _bS = split(_b);
    float _err = ((_m(_aS.x, _bS.x) - _p) + _m(_aS.x, _bS.y) + _m(_aS.y, _bS.x)) + _m(_aS.y, _bS.y);
    return vec2(_p, _err);
}

vec2 _sqrDF(vec2 _a) {
    vec2 _p;
    _p = _twoSqr(_a.x);
    _p.y += 2.0*_m(_a.x, _a.y);
    _p = quickTwoSum(_p.x, _p.y);
    return _p;
}

vec2 _modSqrDF(vec2 _a, vec2 _b) {
    return addDF(_sqrDF(_a), _sqrDF(_b));
}

vec2 _multDF(vec2 _a, vec2 _b) {
    vec2 _p;
    _p = _twoProd(_a.x, _b.x);
    _p.y += _m(_a.x, _b.y);
    _p.y += _m(_a.y, _b.x);
    _p = quickTwoSum(_p.x, _p.y);
    return _p;
}

vec2 _divDF(vec2 _a, vec2 _b) {

    if (_b.x == 0.0 && _b.y == 0.0) { return vec2(1e10); }
    float xn = 1.0/_b.x;
    float _yn = _a.x*xn;
    vec2 _diff = addDF(_a, -_multDF(_b, vec2(_yn, 0.0)));
    vec2 _prod = twoProd(xn, _diff.x);
    return addDF(vec2(_yn, 0.0), _prod);

}



void main() {

    vec4 colorParams = vec4(0.0);

    // use mandelbrot C components
    vec4 xC = addQF(multQF(xScale, vec4(viewPos.x, 0., 0., 0.)), xOffset);
    vec4 yC = addQF(multQF(yScale, vec4(viewPos.y, 0., 0., 0.)), yOffset);

    vec2 MODC = modDF(xC.xy, yC.xy);
    vec4 MODZSQR, MODZSQR_prev = vec4(0.0);
    vec2 MODZ = vec2(0.0);
    vec2 sum, sum_prev = vec2(0.0);

    // use julia C components
//    vec2 xC = xTouchPos;
//    vec2 yC = yTouchPos;

    // use mandelbrot Z components
    vec4 X, Y = vec4(0.);
    vec4 X_prev, Y_prev = vec4(0.);
    vec4 Y_temp;

    vec2 x, y;
    vec2 u, v = vec2(0.);
    vec2 _a, _b = vec2(0.);
    vec2 _modSqr;
    vec2 mod;
    vec2 t;
    float _1 = 1.0 * Sn;
    float _2 = 2.0 * Sn;

    // use julia Z components
//    vec2 X = add(multQF(xScale, vec2(viewPos.x, 0.0)), xOffset);
//    vec2 Y = add(multQF(yScale, vec2(viewPos.y, 0.0)), yOffset);




//    vec4 X1, X2, X3, X4, X5, X6, X7, X8 = vec4(0.);
//    vec4 Y1, Y2, Y3, Y4, Y5, Y6, Y7, Y8 = vec4(0.);

//    float XY1e, XY2e, XY3e, XY4e, XY5e, XY6e, XY7e, XY8e;

//    float eps = 0.005 * xScale.x;
//    bool repeat = false;
//    int period = 0;


    for (int n = 0; n < maxIter; n++) {

        if (n == maxIter - 1) {
            colorParams.w = -1.0;
        }

        // iterate second derivative
//        b = 2.0*(multQF(b, vec2(X.x, Y.x)) + cMult(a, a));

        // iterate derivative -- dual float
//        vec4 t = cMultDF(A, B, X.xy, Y.xy);
//        A = multDF(vec2(2.0, 0.0), t.xy);
//        A.x += 1.0;
//        B = multDF(vec2(2.0, 0.0), t.zw);

        // iterate derivative -- single float
//        x = X.x;
//        y = Y.x;
//        t = 2.0*(x*_b + y*_a);
//        _a = 2.0*(x*_a - y*_b) + _1;
//        _b = t;


        // USE THIS
//        x = X.xy;
//        y = Y.xy;
//        t = multDF(vec2(2.0, 0.0), addDF(multDF(x, _b), multDF(y, _a)));
//        _a = addDF(multDF(vec2(2.0, 0.0), addDF(multDF(x, _a), -multDF(y, _b))), vec2(_1, 0.0));
//        _b = t;





//        X_prev = X;
//        Y_prev = Y;
//        MODZSQR_prev = MODZSQR;

        // iterate z
        Y_temp = mandelbrot_y(X, Y, yC);
        X = mandelbrot_x(X, Y, xC);
        Y = Y_temp;
//        MODZSQR = modSqrQF(X, Y);
        MODZ = modDF(X.xy, Y.xy);




        // check for escape
        if (MODZ.x > R) {

            // normal calculation
//            float lo = 0.5*log(MOD2.x);
//            vec2 Zf = vec2(X.x, Y.x);
//            u = cMult(  cMult(Zf, a), (1.0 + lo)*conj(multQF(a, a)) - lo*conj(multQF(Zf, b))  );

            // normal calculation -- dual float
//            vec4 T = cDivDF(X.xy, Y.xy, A, B);
//            U = T.xy;
//            V = T.zw;
//            vec2 S = modDF(U, V);
//            U = divDF(U, S);
//            V = divDF(V, S);

            // normal calculation -- single float
//            vec2 T = cDivSF(vec2(X.x, Y.x), A);
//            T /= modSF(T);



//            x = X.xy;
//            y = Y.xy;

//            _modSqr = _modSqrDF(_a, _b);
//            u = _divDF(addDF(multDF(x, _a), multDF(y, _b)), _modSqr);
//            v = _divDF(addDF(multDF(y, _a), -multDF(x, _b)), _modSqr);
//            u = divDF(addDF(multDF(x, _a), multDF(y, _b)) * Sp, _modSqr);
//            v = divDF(addDF(multDF(y, _a), -multDF(x, _b)) * Sp, _modSqr);

//            mod = modSqrDF(u, v);
//            u = u / sqrt(mod.x);
//            v = v / sqrt(mod.x);
//            mod = modDF(u, v);
//            u = divDF(u, mod);
//            v = divDF(v, mod);


//            _modSqr = _m(_a.x, _a.x) + _m(_b.x, _b.x);
//            u.x = (X.x*_a.x + Y.x*_b.x) / _modSqr.x;
//            v.x = (Y.x*_a.x - X.x*_b.x) / _modSqr.x;
//            mod.x = sqrt(u.x*u.x + v.x*v.x);
//            u.x /= mod.x;
//            v.x /= mod.x;





            // calculate rays for lighting calculations
//            vec3 normRay = vec3(U.x, V.x, 1.0);
//            vec3 normRay = vec3(T.x, T.y, 1.0);
//            vec3 normRay = vec3(_u * Sp, _v * Sp, 1.0);
//            vec3 normRay = vec3(u.x, v.x, 1.0);

//            normRay = normRay / length(normRay);
//            vec3 lightRay = vec3(lightPos.x, lightPos.y, height);
//            lightRay = lightRay / length(lightRay);
//            vec3 viewRay = vec3(0.0, 0.0, 1.0);
//            vec3 reflectRay = 2.0*dot(normRay, lightRay)*normRay - lightRay;

            // calculate lighting components
//            float diffuse = dot(normRay, lightRay);
//            diffuse = diffuse/(1.0 + height);
//            if (diffuse < 0.0) {
//                diffuse = 0.0;
////                color = vec3(1.0, 0.0, 1.0);
////                break;
//            }
//            float specular = pow(dot(reflectRay, viewRay), 1.5);
//            if (specular < 0.0) {
//                specular = 0.0;
////                color = vec3(1.0, 0.0, 1.0);
////                break;
//            }


            // normalized values -- finite cycles
            //             float m = cmap_cycles*num_colors*(float(i)-log(0.5*log(MOD2.x))/log(2.0))/float(maxIter);
            //             float n = m - (num_colors * floor(m/num_colors));

            // unnormalized values -- infinite cycles
//            float m = float(i);
            float i = float(n)-log(0.5*log(MODZ.x))/log(2.0);
//            float m = float(i)-logDF(multDF(vec2(0.5, 0.0), logDF(MOD2.xy))).x/log(2.0);
//            float n = float(num_colors)/2.0*(cos(m/14.0) + 1.0);
            float j = 0.5*(cos(2.0*pow(i + 5.0, 0.4) -  0.3) + 1.0);

            colorParams.z = j;


//            if      (n >= 0.0 && n < 1.0) {  color = (1.0-n) * c1   +   (n)     * c2;  }
//            else if (n >= 1.0 && n < 2.0) {  color = (2.0-n) * c2   +   (n-1.0) * c3;  }
//            else if (n >= 2.0 && n < 3.0) {  color = (3.0-n) * c3   +   (n-2.0) * c4;  }
//            else if (n >= 3.0 && n < 4.0) {  color = (4.0-n) * c4   +   (n-3.0) * c5;  }
//            else if (n >= 4.0 && n < 5.0) {  color = (5.0-n) * c5   +   (n-4.0) * c1;  }
//            else if (n >= 5.0 && n < 6.0) {  color = (6.0-n) * c6   +   (n-5.0) * c1;  }

//            color = vec3(0.0);

//            color = 2.5*(diffuse + 0.2)*color;
//            color = 1.75*(diffuse + 0.2)*color + 0.75*vec3(specular + 0.01);
//            color = vec3(specular);




            break;

        }



//        if (i > 0) {
//
//            vec4 m_n = absQF(addQF(MODZSQR_prev, -vec4(MODC, 0.0, 0.0)));
//            vec4 M_n = addQF(MODZSQR_prev, vec4(MODC, 0.0, 0.0));
//            vec4 j = addQF(vec4(MODZ, 0.0, 0.0), -m_n);
//            vec4 k = addQF(M_n, -m_n);
//            vec2 t = divDF(j.xy, k.xy);
//            sum_prev = sum;
//            sum = addDF(sum, t);
//
//            if (i == 6) {
//                float q = j.x;
//                if (isnan(q)) {
//                    color = vec3(0.0, 0.0, 1.0);
//                }
//                else {
//                    color = vec3(q);
//                }
//                break;
//            }
//
//        }


    }

    fragmentColor = colorParams;

}
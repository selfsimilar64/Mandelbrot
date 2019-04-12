#version 320 es
#define SPLIT 8193.
#define pi 3.141592654
#define Sn 1e-10
#define Sp 1e10
#define Sh 1e-5
#define R 1e14

precision highp float;
uniform int maxIter;
uniform vec2 xTouchPos;
uniform vec2 yTouchPos;
uniform vec2 xScale;
uniform vec2 yScale;
uniform vec2 xOffset;
uniform vec2 yOffset;

in vec4 viewPos;
out vec4 fragmentColor;




// == UTILITY FUNCTIONS ===========================================================================

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




// == DUAL-FLOAT ARITHMETIC =======================================================================

vec2 add(vec2 a, vec2 b) {
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

vec2 mult(vec2 a, vec2 b) {
    vec2 p;
    p = twoProd(a.x, b.x);
    p.y += a.x * b.y;
    p.y += a.y * b.x;
    p = quickTwoSum(p.x, p.y);
    return p;
}

vec2 sqr(vec2 a) {
    vec2 p;
    p = twoSqr(a.x);
    p.y += 2.0*a.x*a.y;
    p = quickTwoSum(p.x, p.y);
    return p;
}

vec2 sqrtDF(vec2 a) {

    float xn = 1.0/sqrt(a.x);
    float yn = a.x*xn;
    vec2 ynSqr = sqr(vec2(yn, 0.0));

    float diff = (add(a, -ynSqr)).x;
    vec2 prod = mult(twoProd(xn, diff), vec2(0.5, 0.0));

    return add(vec2(yn, 0.0), prod);

}

vec2 divDF(vec2 a, vec2 b) {

    float xn = 1.0/b.x;
    float yn = a.x*xn;
    float diff = add(a, -mult(b, vec2(yn, 0.0))).x;
    vec2 prod = twoProd(xn, diff);
    return add(vec2(yn, 0.0), prod);

}

vec2 modSqrDF(vec2 X, vec2 Y) {
        return add(sqr(X), sqr(Y));
//    return mult(sqr(X), add(vec2(1.0, 0.0), sqr(divDF(Y, X))));
}

vec2 modDF(vec2 a, vec2 b) {
//    return vec2(sqrt(modSqrDF(a, b).x), 0.0);
    return sqrtDF(modSqrDF(a, b));
}

vec2 modDF2(vec2 a, vec2 b) {
    return mult(a, sqrtDF(add(vec2(1.0, 0.0), sqr(divDF(b, a)))));
}

vec2 absDF(vec2 a) {
    if (a.x < 0.0) { return -a; }
    else { return a; }
}

vec2 mandelbrot_x(vec2 X, vec2 Y, vec2 xC) {
    return add(add(sqr(X), -sqr(Y)), xC);
}

vec2 mandelbrot_y(vec2 X, vec2 Y, vec2 yC) {
    vec2 T = mult(X, Y);
    return add(mult(vec2(2.0, 0.0), T), yC);
}




// == QUAD-FLOAT ARITHMETIC =======================================================================

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




// == SHIFTED DUAL-FLOAT ARITHMETIC ===============================================================

float _m(float _a, float _b) {
    return (_a*_b) * Sp;
}

vec2 _twoProd(float _a, float _b) {
    float _p = _m(_a, _b);
    vec2 _aS = split(_a);
    vec2 _bS = split(_b);
    float _e = ((_m(_aS.x, _bS.x) - _p) + _m(_aS.x, _bS.y) + _m(_aS.y, _bS.x)) + _m(_aS.y, _bS.y);
    return vec2(_p, _e);
}

vec2 _mult(vec2 _a, vec2 _b) {
    vec2 _p = _twoProd(_a.x, _b.x);
    _p.y += _m(_a.x, _b.y);
    _p.y += _m(_a.y, _b.x);
    _p = quickTwoSum(_p.x, _p.y);
    return _p;
}

vec2 _twoSqr(float _a) {
    float _p = _m(_a, _a);
    vec2 _s = split(_a);
    float _e = ((_m(_s.x, _s.x) - _p) + 2.0*_m(_s.x, _s.y)) + _m(_s.y, _s.y);
    return vec2(_p, _e);
}

vec2 _sqr(vec2 _a) {
    vec2 _p = _twoSqr(_a.x);
    _p.y += 2.0*_m(_a.x, _a.y);
    _p = quickTwoSum(_p.x, _p.y);
    return _p;
}

vec2 _divDF(vec2 _a, vec2 _b) {

    float _xn = (1.0/_b.x) * Sn * Sn;
    float _yn = _m(_a.x, _xn);
    float _diff = add(_a, -_mult(_b, vec2(_yn, 0.0))).x;
    vec2 _prod = _twoProd(_xn, _diff);
    return add(vec2(_yn, 0.0), _prod);

}

vec2 _modSqrDF(vec2 _X, vec2 _Y) {
    return add(_sqr(_X), _sqr(_Y));
}

vec2 _sqrtDF(vec2 _a) {

    float _xn = (1.0/(sqrt(_a.x) * Sh)) * Sn * Sn;
    float _yn = _m(_a.x, _xn);
    vec2 _ynSqr = _sqr(vec2(_yn, 0.0));

    float _diff = (add(_a, -_ynSqr)).x;
    vec2 prod = mult(_twoProd(_xn, _diff), vec2(0.5, 0.0));     // change to _mult?

    return add(vec2(_yn, 0.0), prod);

}

vec2 _modDF(vec2 _a, vec2 _b) {
    return _sqrtDF(_modSqrDF(_a, _b));
}

vec2 _mandelbrot_x(vec2 _X, vec2 _Y, vec2 _xC) {
    return add(add(_sqr(_X), -_sqr(_Y)), _xC);
}

vec2 _mandelbrot_y(vec2 _X, vec2 _Y, vec2 _yC) {
    vec2 _T = _mult(_X, _Y);
    return add(add(_T, _T), _yC);
}




// == COMPLEX ARITHMETIC ==========================================================================

vec2 conj(vec2 p) {
    return vec2(p.x, -p.y);
}

float modSqrSF(vec2 p) {
    return p.x*p.x + p.y*p.y;
}

vec2 cMultSF(vec2 p, vec2 q) {
    return vec2(p.x*q.x - p.y*q.y, p.y*q.x + p.x*q.y);
}

vec2 div(vec2 p, vec2 q) {
    return cMultSF(p, conj(q)) / modSqrSF(q);
}

float modulus(vec2 p) {
    return sqrt(p.x*p.x + p.y*p.y);
}

vec4 cAddDF(vec2 X, vec2 Y, vec2 P, vec2 Q) {
    vec2 U = add(X, P);
    vec2 V = add(Y, Q);
    return vec4(U, V);
}

vec4 cMultDF(vec2 X, vec2 Y, vec2 A, vec2 B) {

    vec2 k1 = mult(A, add(X, Y));
    vec2 k2 = mult(X, add(B, -A));
    vec2 k3 = mult(Y, add(A, B));
    vec2 U = add(k1, -k3);
    vec2 V = add(k1, k2);
    return vec4(U, V);

}

vec4 cDivDF(vec2 X, vec2 Y, vec2 P, vec2 Q) {
    vec4 C = cMultDF(X, Y, P, -Q);
    vec2 MODSQR = modSqrDF(P, Q);
    vec2 U = divDF(C.xy, MODSQR);
    vec2 V = divDF(C.zw, MODSQR);
    return vec4(U, V);
}

vec4 test(vec2 X, vec2 Y, vec2 xD, vec2 yD, vec2 xC, vec2 yC) {

    vec2 U = add(sqr(X), -sqr(Y));
    vec2 V = mult(vec2(2.0, 0.0), mult(X, Y));

    vec2 P = add(add(mult(xD, X), -mult(yD, Y)), vec2(1.0, 0.0));
    vec2 Q = add(mult(xD, Y), mult(yD, X));

    vec4 S = cDivDF(U, V, P, Q);
    S.xy = add(S.xy, xC);
    S.zw = add(S.zw, yC);

    return S;

}




void main() {

    // use mandelbrot C components
    vec2 xC = add(mult(xScale, vec2(viewPos.x, 0.0)), xOffset);
    vec2 yC = add(mult(yScale, vec2(viewPos.y, 0.0)), yOffset);
    vec2 MODC = modDF(xC, yC);
    float ARGC = atan(yC.x, xC.x);

//    xC = vec2(1.4686, 0.0);
//    yC = vec2(1.265, 0.0);

    // use julia C components
//    vec2 xC = xTouchPos;
//    vec2 yC = yTouchPos;

    // use mandelbrot Z components
    vec2 X, Y = vec2(0.0);
    float xSqr, ySqr;
    vec2 X1, Y1 = vec2(0.0);
    vec2 X2, Y2 = vec2(0.0);
    vec2 Y_temp;
    vec2 MODZ, MODSQRZ, MODSQRZ1 = vec2(0.);
    vec2 sum, sum1 = vec2(0.0);

    // use julia Z components
//    vec2 X = add(mult(xScale, vec2(viewPos.x, 0.0)), xOffset);
//    vec2 Y = add(mult(yScale, vec2(viewPos.y, 0.0)), yOffset);


    float num_colors = 5.0;
    float cmap_cycles = 10.0;

    vec2 lightPos = vec2(1.0);
    float height = 1.25;


    float il = 1.0/log(2.0);
    float lp = log(log(R)/2.0);


    // *PERIOD CHECKING*
//    vec2 X1, X2, X3, X4, X5, X6, X7, X8 = vec2(0.0);
//    vec2 Y1, Y2, Y3, Y4, Y5, Y6, Y7, Y8 = vec2(0.0);

//    float XY1e, XY2e, XY3e, XY4e, XY5e, XY6e, XY7e, XY8e;

//    float eps = 0.001 * xScale.x;
//    bool repeat = false;
//    int period = 0;



    vec2 a = vec2(0.0, 0.0);
    vec2 b = vec2(0.0, 0.0);
    vec2 u = vec2(0.0, 0.0);

    float q;

    vec3 color    =  vec3(0.0, 0.0, 0.0);
    vec3 black    =  vec3(0.0, 0.0, 0.0);
    vec3 purple   =  vec3(0.3, 0.0, 0.5);
    vec3 red      =  vec3(1.0, 0.0, 0.0);
    vec3 pink     =  vec3(1.0, 0.3, 0.4);
    vec3 yellow   =  vec3(1.0, 1.0, 0.0);
    vec3 white    =  vec3(1.0, 1.0, 1.0);
    vec3 darkblue =  vec3(0.0, 0.15, 0.25);
    vec3 orange   =  vec3(1.0, 0.6, 0.0);
    vec3 turquoise =  vec3(64.0, 224.0, 208.0) / 255.0;
    vec3 magenta = vec3(1.0, 0.0, 1.0);


    // darkblue, white, orange, purple

    vec3 c1 = vec3(0.0, 0.1, 0.2);
    vec3 c2 = darkblue;
    vec3 c3 = vec3(0.7);
    vec3 c4 = vec3(0.9, 0.4, 0.2);
    vec3 c5 = purple * 0.5;

    vec3 yellowish = vec3(0.9, 0.95, 0.1);    // yellow-ish
    vec3 darkblue2 = vec3(0.11, 0.188, 0.35);  // dark blue
    vec3 grass = vec3(0.313, 0.53, 0.45);  // grass

//    vec3 c1 = 0.6*turquoise;
//    vec3 c2 = purple;
//    vec3 c3 = black;
//    vec3 c4 = vec3(0.8, 0.0, 0.3);
//    vec3 c5 = white;


    for (int n = 0; n < maxIter; n++) {

        q = float(n)/float(maxIter - 1);

        if (n == maxIter - 1) {
            color = vec3(0.0);
        }


        // == NORMAL MAP -- LOOP ================================================================
        // iterate second derivative
//        b = 2.0*(cMultSF(b, vec2(X.x, Y.x)) + cMultSF(a, a));

        // iterate derivative
//        a = 2.0*cMultSF(a, vec2(X.x, Y.x));
//        a.x = a.x + 1.0;




        // cycle previous values
//        X8 = X7;
//        Y8 = Y7;
//
//        X7 = X6;
//        Y7 = Y6;
//
//        X6 = X5;
//        Y6 = Y5;
//
//        X5 = X4;
//        Y5 = Y4;
//
//        X4 = X3;
//        Y4 = Y3;
//
//        X3 = X2;
//        Y3 = Y2;
//
//        X2 = X1;
//        Y2 = Y1;
//
//        X1 = X;
//        Y1 = Y;


        X2 = X1;
        Y2 = Y1;
        X1 = X;
        Y1 = Y;

        MODSQRZ1 = MODSQRZ;

        // iterate z
        Y_temp = mandelbrot_y(X, Y, yC);
        X = mandelbrot_x(X, Y, xC);
        Y = Y_temp;
//        vec4 S = test(X, Y, vec2(-0.2013, 0.0), vec2(0.5638, 0.0), xC, yC);
//        X = S.xy;
//        Y = S.zw;

        MODSQRZ = modSqrDF(X, Y);
        MODZ = modDF(X, Y);



//        XY1e = abs(X.x - X1.x) + abs(X.y - X1.y) + abs(Y.x - Y1.x) + abs(Y.y - Y1.y);
//        XY2e = abs(X.x - X2.x) + abs(X.y - X2.y) + abs(Y.x - Y2.x) + abs(Y.y - Y2.y);
//        XY3e = abs(X.x - X3.x) + abs(X.y - X3.y) + abs(Y.x - Y3.x) + abs(Y.y - Y3.y);
//        XY4e = abs(X.x - X4.x) + abs(X.y - X4.y) + abs(Y.x - Y4.x) + abs(Y.y - Y4.y);
//        XY5e = abs(X.x - X5.x) + abs(X.y - X5.y) + abs(Y.x - Y5.x) + abs(Y.y - Y5.y);
//        XY6e = abs(X.x - X6.x) + abs(X.y - X6.y) + abs(Y.x - Y6.x) + abs(Y.y - Y6.y);
//        XY7e = abs(X.x - X7.x) + abs(X.y - X7.y) + abs(Y.x - Y7.x) + abs(Y.y - Y7.y);
//        XY8e = abs(X.x - X8.x) + abs(X.y - X8.y) + abs(Y.x - Y8.x) + abs(Y.y - Y8.y);
//
//        if (n > 8) {
//        // check for periodicity
//            if (XY1e < eps) {
//                period = 1;
//                repeat = true;
//            }
//            if (XY2e < eps) {
//                period = 2;
//                repeat = true;
//            }
//            if (XY3e < eps) {
//                period = 3;
//                repeat = true;
//            }
//            if (XY4e < eps) {
//                period = 4;
//                repeat = true;
//            }
//            if (XY5e < eps) {
//                period = 5;
//                repeat = true;
//            }
//            if (XY6e < eps) {
//                period = 6;
//                repeat = true;
//            }
//            if (XY7e < eps) {
//                period = 7;
//                repeat = true;
//            }
//            if (XY8e < eps) {
//                period = 8;
//                repeat = true;
//            }
//        }
//
//        if (repeat) {
//            // color = vec3(float(i)/float(maxIter));
//            // color = vec3(1.0, 0.0, 1.0);
//            color = vec3(0.0);
//            break;
//        }




        xSqr = X.x*X.x;
        ySqr = Y.x*Y.x;

        // check for escape
        if (MODZ.x > R || isinf(xSqr) || isinf(ySqr) || isinf(xSqr + ySqr)) {


            // == NORMAL MAP -- FINAL ===========================================================

            // normal calculation
//            vec2 Zf = vec2(X.x, Y.x);
//            u = div(Zf, a);
//            u = u/modulus(u);
//            color.xy = 0.5*(u + 1.0);       // map from [-1, 1] to [0, 1] to avoid clipping




            // == TRIANGLE INEQUALITY -- FINAL ==================================================

            sum = mult(sum, vec2(1.0/(float(n) - 1.0), 0.0));
            sum1 = mult(sum1, vec2(1.0/(float(n) - 2.0), 0.0));
            float s = il*lp - il*log(log(modDF(X1, Y1).x));
            float r = sum1.x + (sum.x - sum1.x)*(s + 1.0);
//            r /= pi;
//            float q = mod(cmap_cycles*num_colors*r, num_colors);

            color.z = r;
//            color.z = float(n)/float(maxIter);


            // normalized values -- finite cycles
//            float p = float(n)/float(maxIter) + 1.0;
//             float m = cmap_cycles*num_colors*(float(i)-log(0.5*log(MOD2.x))/log(2.0))/float(maxIter);
            float p = (float(n) - log(0.25*log(MODSQRZ1.x))/log(2.0)) / float(maxIter);
//            color.z = p;
//            float n = m - (num_colors * floor(m/num_colors));

            // unnormalized values -- infinite cycles
//            float p = float(n)-log(0.25*log(MODSQRZ1.x))/log(2.0);
//            float p = float(n) + 1.0 + 1.0/log(2.0)*log(log(R)/log(MODZ.x));
//            float n = float(num_colors)/2.0*(cos(m/14.0) + 1.0);
//            q = 0.5*(cos(2.0*pow(p + 5.0, 0.4) -  0.3) + 1.0);
//            float q = r*num_colors;



//            color.z = q;


            // == COLORMAP ======================================================================

//            if      (q >= 0.0 && q < 1.0) {  color = (1.0-q) * c1   +   (q)     * c2;  }
//            else if (q >= 1.0 && q < 2.0) {  color = (2.0-q) * c2   +   (q-1.0) * c3;  }
//            else if (q >= 2.0 && q < 3.0) {  color = (3.0-q) * c3   +   (q-2.0) * c4;  }
//            else if (q >= 3.0 && q < 4.0) {  color = (4.0-q) * c4   +   (q-3.0) * c5;  }
//            else if (q >= 4.0 && q < 5.0) {  color = (5.0-q) * c5   +   (q-4.0) * c1;  }





            break;

        }


        // == TRIANGLE INEQUALITY -- LOOP =======================================================

//        if (n > 0) {
//            vec2 m_n = absDF(add(MODSQRZ1, -MODC));
//            vec2 M_n = add(MODSQRZ1, MODC);
//            vec2 j = add(MODZ, -m_n);
//            vec2 k = add(M_n, -m_n);
//            vec2 t = divDF(j, k);
//            sum1 = sum;
//            sum = add(sum, t);
//        }




        // == CURVATURE -- LOOP =================================================================

//        if (n > 1) {
//            vec4 A = cAddDF(X, Y, -X1, -Y1);
//            vec4 B = cAddDF(X1, Y1, -X2, -Y2);
//            vec4 t = cDivDF(A.xy, A.zw, B.xy, B.zw);
//            sum1 = sum;
//            sum.x += abs(atan(t.z, t.x));
//        }



        // == STRIPE -- LOOP ====================================================================

        sum1 = sum;
        float ARGZ = atan(Y.x, X.x);
        sum.x += 0.5*(sin(3.0*ARGZ) + 1.0);





    }

    fragmentColor = vec4(color, q);

}
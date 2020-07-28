#version 300 es

precision highp float;
precision highp int;

const float SPLIT = 8193.0;
const float pi = 3.141592654;
const float phi = 1.618033988;
const float pz = 1e-45;
const float Sn = 1e-8;
const float Sp = 1e8;
const float Sh = 1e4;
const vec2 ZERO = vec2(0.0);
const vec2 ONE = vec2(1.0, 0.0);
const vec2 I = vec2(0.0, 1.0);

const vec2 _pi = vec2(3.141593e+00, -8.742278e-08);
const vec2 _2pi = vec2(6.283185e+00, -1.748456e-07);
const vec2 _pi2 = vec2(1.570796e+00, -4.371139e-08);
const vec2 _pi4 = vec2(7.853982e-01, -2.185569e-08);
const vec2 _3pi4 = vec2(2.356194e+00, -5.962440e-09);
const vec2 _e = vec2(2.718282e+00, 8.254840e-08);
const vec2 _log2 = vec2(6.931472e-01, -1.904654e-09);
const vec2 _log10 = vec2(2.302585e+00, -3.197544e-08);
const vec2 _pi16 = vec2(1.963495e-01, -5.463924e-09);
const float _eps = 4.93038065763132e-32;  // 2^-104

/* Table of sin(k * pi/16) and cos(k * pi/16). */
const vec2 sin_table[4] = vec2[4](
    vec2(1.950903e-01, -1.670472e-09),
    vec2(3.826834e-01,  6.223351e-09),
    vec2(5.555702e-01, -1.176952e-08),
    vec2(7.071068e-01,  1.210162e-08)
);

const vec2 cos_table[4] = vec2[4](
    vec2(9.807853e-01, 2.973947e-08),
    vec2(9.238795e-01, 2.830749e-08),
    vec2(8.314696e-01, 1.687026e-08),
    vec2(7.071068e-01, 1.210162e-08)
);

const int n_inv_fact = 15;
const vec2 inv_fact[15] = vec2[15](
    vec2(1.666667e-01, -4.967054e-09),
    vec2(4.166667e-02, -1.241763e-09),
    vec2(8.333334e-03, -4.346172e-10),
    vec2(1.388889e-03, -3.363109e-11),
    vec2(1.984127e-04, -2.725597e-12),
    vec2(2.480159e-05, -3.406996e-13),
    vec2(2.755732e-06, 3.793571e-14),
    vec2(2.755732e-07, -7.575112e-15),
    vec2(2.505211e-08, 4.417623e-16),
    vec2(2.087676e-09, 1.108284e-16),
    vec2(1.605904e-10, -5.352527e-18),
    vec2(1.147075e-11, 2.372208e-19),
    vec2(7.647164e-13, 1.220071e-20),
    vec2(4.779477e-14, 7.625444e-22),
    vec2(2.811457e-15, -1.046208e-22)
);


uniform uint maxIter;
uniform float R;
uniform float power;
uniform float x0;
uniform float y0;
uniform vec2 j;
uniform vec2 p1;
uniform vec2 p2;
uniform vec2 p3;
uniform vec2 p4;
uniform float q1;
uniform float q2;
uniform vec2 xScale;
uniform vec2 yScale;
uniform vec2 xCoord;
uniform vec2 yCoord;
uniform float sinRotate;
uniform float cosRotate;

in vec4 viewPos;
out uvec2 fragmentColor;





vec2 quickTwoSum(float a, float b) {
    float s = a + b + pz;
    float v = s - a;
    float e = b - v;
    return vec2(s, e);
}

vec2 twoSum(float a, float b) {
    float s = a + b + pz;
    float v = s - a + pz;
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
    float q = t - a + pz;
    float a_hi = t - q;
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
vec2 mult(float a, vec2 b) { return mult(vec2(a, 0.0), b); }

vec2 sqr(vec2 a) {
    vec2 p;
    p = twoSqr(a.x);
    p.y += 2.0*a.x*a.y;
    p = quickTwoSum(p.x, p.y);
    return p;
}

vec2 sqrtd(vec2 a) {
    float xn = 1.0/sqrt(a.x);
    float yn = a.x*xn;
    vec2 ynSqr = sqr(vec2(yn, 0.0));
    float diff = (add(a, -ynSqr)).x;
    vec2 prod = 0.5*twoProd(xn, diff);
    return add(vec2(yn, 0.0), prod);
}

vec2 div(vec2 a, vec2 b) {
    float xn = 1.0/b.x;
    float yn = a.x*xn;
    float diff = add(a, -mult(b, vec2(yn, 0.0))).x;
    vec2 prod = twoProd(xn, diff);
    return add(vec2(yn, 0.0), prod);
}

vec2 inv(vec2 a) {
    float xn = 1.0/a.x;
    float diff = add(ONE, -mult(a, vec2(xn, 0.0))).x;
    vec2 prod = twoProd(xn, diff);
    return add(vec2(xn, 0.0), prod);
}

vec2 absd(vec2 a) {
    if (a.x < 0.0) { return -a; }
    else { return a; }
}

bool gtd(vec2 a, float b) {
    if (a.x > b || (a.x == b && a.y > 0.0)) return true;
    else return false;
}

vec2 sind_taylor(vec2 a) {

    float thresh = 0.5*abs(a.x)*_eps;
    vec2 r, s, t, x;

    if (a == ZERO) { return ZERO; }

    int i = 0;
    x = -sqr(a);
    s = a;
    r = a;
    do {
        r = mult(r, x);
        t = mult(r, inv_fact[i]);
        s = add(s, t);
        i += 2;
    } while (i < n_inv_fact && abs(t.x) > thresh);

    return s;
}

vec2 cosd_taylor(vec2 a) {

    float thresh = 0.5*_eps;
    vec2 r, s, t, x;

    if (a == ZERO) { return ONE; }

    x = -sqr(a);
    r = x;
    s = add(ONE, 0.5*r);
    int i = 1;
    do {
        r = mult(r, x);
        t = mult(r, inv_fact[i]);
        s = add(s, t);
        i += 2;
    } while (i < n_inv_fact && abs(t.x) > thresh);

    return s;

}

void sincosd_taylor(vec2 a, out vec2 sin_a, out vec2 cos_a) {

    if (a == ZERO) {
        sin_a = ZERO;
        cos_a = ONE;
        return;
    }

    sin_a = sind_taylor(a);
    cos_a = sqrtd(add(ONE, -sqr(sin_a)));
    //cos_a = cosd_taylor(a);

}

vec2 roundd(vec2 a) {
    if (a.x == 0.5) {
        if (a.y > 0.0) return vec2(ceil(a.x), 0.0);
        else return vec2(floor(a.x), 0.0);
    }
    else {
        if (mod(a.x, 1.0) > 0.5) return vec2(ceil(a.x), 0.0);
        else return vec2(floor(a.x), 0.0);
    }
}

vec2 sind(vec2 a) {

    /* Strategy.  To compute sin(x), we choose integers a, b so that

         x = s + a * (pi/2) + b * (pi/16)

       and |s| <= pi/32.  Using the fact that

         sin(pi/16) = 0.5 * sqrt(2 - sqrt(2 + sqrt(2)))

       we can compute sin(x) from sin(s), cos(s).  This greatly
       increases the convergence of the sine Taylor series. */

    if (a == ZERO) { return ZERO; }

    // approximately reduce modulo 2*pi
    vec2 z = vec2(round(div(a, _2pi).x), 0.0);
    vec2 r = add(a, -mult(_2pi, z));

    // approximately reduce modulo pi/2 and then modulo pi/16.
    vec2 t;
    float q = floor(r.x / _pi2.x + 0.5);
    t = add(r, -mult(q, _pi2));
    int j = int(q);
    q = floor(t.x / _pi16.x + 0.5);
    t = add(t, -mult(q, _pi16));
    int k = int(q);
    int abs_k = abs(k);


    // errors
    if (j < -2 || j > 2) { return ZERO; }
    if (abs_k > 4) { return ZERO; }


    if (k == 0) {
        if (j == 0) return sind_taylor(t);
        else if (j == 1) return cosd_taylor(t);
        else if (j == -1) return -cosd_taylor(t);
        else return -sind_taylor(t);
    }

    vec2 u = cos_table[abs_k - 1];
    vec2 v = sin_table[abs_k - 1];
    vec2 sin_t, cos_t;
    sincosd_taylor(t, sin_t, cos_t);
    if (j == 0) {
        if (k > 0) { r = add(mult(u, sin_t), mult(v, cos_t)); }
        else { r = add(mult(u, sin_t), -mult(v, cos_t)); }
    }
    else if (j == 1) {
        if (k > 0) { r = add(mult(u, cos_t), -mult(v, sin_t)); }
        else { r = add(mult(u, cos_t), mult(v, sin_t)); }
    }
    else if (j == -1) {
        if (k > 0) { r = add(mult(v, sin_t), -mult(u, cos_t)); }
        else if (k < 0) { r = add(mult(-u, cos_t), -mult(v, sin_t)); }
    }
    else {
        if (k > 0) { r = add(mult(-u, sin_t), -mult(v, cos_t)); }
        else { r = add(mult(v, cos_t), -mult(u, sin_t)); }
    }

    return r;

}

vec2 cosd(vec2 a) {

    if (a == ZERO) { return ONE; }
    
    // approximately reduce modulo 2*pi
    vec2 z = vec2(round(div(a, _2pi).x), 0.0);
    vec2 r = add(a,  -mult(z, _2pi));
    
    // approximately reduce modulo pi/2 and then modulo pi/16
    vec2 t;
    float q = floor(r.x / _pi2.x + 0.5);
    t = add(r, -mult(q, _pi2));
    int j = int(q);
    q = floor(t.x / _pi16.x + 0.5);
    t = add(t, -mult(q, _pi16));
    int k = int(q);
    int abs_k = abs(k);
    
    if (j < -2 || j > 2) { return ZERO; }
    if (abs_k > 4) { return ZERO; }
    
    if (k == 0) {
        if (j == 0) return cosd_taylor(t);
        else if (j == 1) return -sind_taylor(t);
        else if (j == -1) return sind_taylor(t);
        else return -cosd_taylor(t);
    }
    
    vec2 sin_t, cos_t;
    sincosd_taylor(t, sin_t, cos_t);
    vec2 u = cos_table[abs_k - 1];
    vec2 v = sin_table[abs_k - 1];
    
    if (j == 0) {
        if (k > 0) { r = add(mult(u, cos_t), -mult(v, sin_t)); }
        else { r = add(mult(u, cos_t), mult(v, sin_t)); }
    }
    else if (j == 1) {
        if (k > 0) { r = add(-mult(u, sin_t), -mult(v, cos_t)); }
        else { r = add(mult(v, cos_t), -mult(u, sin_t)); }
    }
    else if (j == -1) {
        if (k > 0) { r = add(mult(u, sin_t), mult(v, cos_t)); }
        else { r = add(mult(u, sin_t), -mult(v, cos_t)); }
    }
    else {
        if (k > 0) { r = add(mult(v, sin_t), -mult(u, cos_t)); }
        else { r = add(-mult(u, cos_t), -mult(v, sin_t)); }
    }
    
    return r;

}

void sincosd(vec2 a, out vec2 sin_a, out vec2 cos_a) {

    if (a == ZERO) {
        sin_a = ZERO;
        cos_a = ONE;
        return;
    }
    
    // approximately reduce modulo 2*pi
    vec2 z = vec2(round(div(a, _2pi).x), 0.0);
    vec2 r = add(a, -mult(_2pi, z));
    
    // approximately reduce module pi/2 and pi/16
    vec2 t;
    float q = floor(r.x / _pi2.x + 0.5);
    t = add(r, -mult(q, _pi2));
    int j = int(q);
    int abs_j = abs(j);
    q = floor(t.x / _pi16.x + 0.5);
    t = add(t, -mult(q, _pi16));
    int k = int(q);
    int abs_k = abs(k);
    
    if (abs_j > 2) {
        cos_a = ZERO;
        sin_a = ZERO;
        return;
    }
    
    if (abs_k > 4) {
        cos_a = ZERO;
        sin_a = ZERO;
        return;
    }
    
    vec2 sin_t, cos_t;
    vec2 s, c;
    
    sincosd_taylor(t, sin_t, cos_t);
    
    if (abs_k == 0) {
        s = sin_t;
        c = cos_t;
    }
    else {
        vec2 u = cos_table[abs_k - 1];
        vec2 v = sin_table[abs_k - 1];
    
        if (k > 0) {
            s = add(mult(u, sin_t), mult(v, cos_t));
            c = add(mult(u, cos_t), -mult(v, sin_t));
        } else {
            s = add(mult(u, sin_t), -mult(v, cos_t));
            c = add(mult(u, cos_t), mult(v, sin_t));
        }
    }
    
    if (abs_j == 0) {
        sin_a = s;
        cos_a = c;
    } else if (j == 1) {
        sin_a = c;
        cos_a = -s;
    } else if (j == -1) {
        sin_a = -c;
        cos_a = s;
    } else {
        sin_a = -s;
        cos_a = -c;
    }

}

vec2 expd(vec2 a) {

    /* Strategy:  We first reduce the size of x by noting that
       
            exp(kr + m * log(2)) = 2^m * exp(r)^k
    
       where m and k are integers.  By choosing m appropriately
       we can make |kr| <= log(2) / 2 = 0.347.  Then exp(r) is 
       evaluated using the familiar Taylor series.  Reducing the 
       argument substantially speeds up the convergence.       */
    
    float k = 512.0;
    float inv_k = 1.0 / k;
    
    if (a.x <= -709.0) return ZERO;
    if (a.x >=  709.0) return ZERO; // inf
    if (a == ZERO) return ONE;
    if (a == ONE) return _e;
    
    float m = floor(a.x / _log2.x + 0.5);
    vec2 r = inv_k*add(a, -mult(m, _log2));
    vec2 s, t, p;
    
    p = sqr(r);
    s = add(r, 0.5*p);
    p = mult(p, r);
    t = mult(p, inv_fact[0]);
    int i = 0;
    do {
        s = add(s, t);
        p = mult(p, r);
        ++i;
        t = mult(p, inv_fact[i]);
    } while (abs(t.x) > inv_k*_eps && i < 5);
    
    s = add(s, t);
    
    s = add(2.0*s, sqr(s));
    s = add(2.0*s, sqr(s));
    s = add(2.0*s, sqr(s));
    s = add(2.0*s, sqr(s));
    s = add(2.0*s, sqr(s));
    s = add(2.0*s, sqr(s));
    s = add(2.0*s, sqr(s));
    s = add(2.0*s, sqr(s));
    s = add(2.0*s, sqr(s));
    s = add(s, ONE);
    
    return exp2(m)*s;

}

vec2 logd(vec2 a) {

    /* Strategy.  The Taylor series for log converges much more
         slowly than that of exp, due to the lack of the factorial
         term in the denominator.  Hence this routine instead tries
         to determine the root of the function

             f(x) = exp(x) - a

         using Newton iteration.  The iteration is given by

             x' = x - f(x)/f'(x)
                = x - (1 - a * exp(-x))
                = x + a * exp(-x) - 1.

         Only one iteration is needed, since Newton's iteration
         approximately doubles the number of digits per iteration. */

    if (a == ONE) { return ZERO; }
    if (a.x <= 0.0) { return ZERO; } // error

    vec2 x = vec2(log(a.x), 0.0);

    x = add(add(x, mult(a, expd(-x))), -ONE);
    return x;

}

vec2 powd(vec2 a, vec2 b) { return expd(mult(b, logd(a))); }

vec2 modd(vec2 a, float b) {
    int n = int(div(a, vec2(b, 0.0)).x);  // optimize ??
    return add(a, -mult(b, vec2(float(n), 0.0)));
}





bool isSpecial(float a) { return isinf(a) || isnan(a); }

vec2 complex(float a) { return vec2(a, 0.0); }
vec4 complex(vec2 a) { return vec4(a.xy, ZERO); }

vec2 _double(float a) { return vec2(a, 0.0); }
vec4 _double(vec2 z) { return vec4(z.x, 0.0, z.y, 0.0); }

vec4 cadd(vec4 z, float a) {
    return vec4(add(z.xy, vec2(a, 0.0)), z.zw);
}
vec4 cadd(float a, vec4 z) {
    return cadd(z, a);
}
vec4 cadd(vec4 z, vec4 w) {
    vec2 u = add(z.xy, w.xy);
    vec2 v = add(z.zw, w.zw);
    return vec4(u, v);
}
vec4 cadd(vec4 z, vec2 w) {
    return cadd(z, vec4(w.x, 0.0, w.y, 0.0));
}
vec4 cadd(vec2 w, vec4 z) {
    return cadd(z, w);
}

float atan2(vec2 z) {
    return atan(z.y, z.x);
}
vec2 atan2(vec2 y, vec2 x) {

    /* Strategy: Instead of using Taylor series to compute
     arctan, we instead use Newton's iteration to solve
     the equation

        sin(z) = y/r    or    cos(z) = x/r

     where r = sqrt(x^2 + y^2).
     The iteration is given by

        z' = z + (y - sin(z)) / cos(z)          (for equation 1)
        z' = z - (x - cos(z)) / sin(z)          (for equation 2)

     Here, x and y are normalized so that x^2 + y^2 = 1.
     If |x| > |y|, then first iteration is used since the
     denominator is larger.  Otherwise, the second is used.
  */

    if (x == ZERO) {
        if (y == ZERO) { return ZERO; }
        if (gtd(y, 0.0)) return _pi2;
        else return -_pi2;
    }
    else if (y == ZERO) {
        if (gtd(x, 0.0)) return ZERO;
        else return _pi;
    }

    if (x == y) {
        if (gtd(y, 0.0)) return _pi4;
        else return -_3pi4;
    }

    if (x == -y) {
        if (gtd(y, 0.0)) return _3pi4;
        else return -_pi4;
    }

    vec2 r = sqrtd(add(sqr(x), sqr(y)));
    vec2 xx = div(x, r);
    vec2 yy = div(y, r);

    /* Compute double precision approximation to atan. */
    vec2 z = vec2(atan(y.x, x.x), 0.0);
    vec2 sin_z, cos_z;

    if (abs(xx.x) > abs(yy.x)) {
        /* Use Newton iteration 1.  z' = z + (y - sin(z)) / cos(z)  */
        sincosd(z, sin_z, cos_z);
        z = add(z, div(add(yy, -sin_z), cos_z));
    }
    else {
        /* Use Newton iteration 2.  z' = z - (x - cos(z)) / sin(z)  */
        sincosd(z, sin_z, cos_z);
        z = add(z, -div(add(xx, -cos_z), sin_z));
    }

    return z;

}

vec2 conj(vec2 z) {
    return vec2(z.x, -z.y);
}
vec4 conj(vec4 z) { return vec4(z.xy, -z.zw); }

vec2 cmult(vec2 z, vec2 w) {
    return vec2(z.x*w.x - z.y*w.y, z.x*w.y + w.x*z.y);
}
vec4 cmult(float a, vec4 z) {
    vec2 b = vec2(a, 0.0);
    return vec4(mult(b, z.xy), mult(b, z.zw));
}
vec4 cmult(vec4 z, float a) {
    return cmult(a, z);
}
vec4 cmult(vec4 z, vec4 w) {
    vec2 x = z.xy;
    vec2 y = z.zw;
    vec2 u = w.xy;
    vec2 v = w.zw;
    vec2 p = add(mult(x, u), -mult(y, v));
    vec2 q = add(mult(x, v), mult(y, u));
    return vec4(p, q);
}
vec4 cmult(vec2 z, vec4 w) { return cmult(_double(z), w); }
vec4 cmult(vec4 w, vec2 z) { return cmult(z, w); }

float sqr(float a) {
    return a*a;
}
vec2 csqr(vec2 z) {
    return vec2(z.x*z.x - z.y*z.y, 2.0*z.x*z.y);
}
vec4 csqr(vec4 z) {
    vec2 x = z.xy;
    vec2 y = z.zw;
    vec2 u = add(sqr(x), -sqr(y));
    vec2 v = 2.0*mult(x, y);
    return vec4(u, v);
}

float cube(float a) {
    return a*a*a;
}
vec2 ccube(vec2 z) {
    float xsqr = z.x*z.x;
    float ysqr = z.y*z.y;
    return vec2(
    z.x*(xsqr - 3.0*ysqr),
    z.y*(3.0*xsqr - ysqr)
    );
}
vec4 ccube(vec4 z) {
    vec2 x = z.xy;
    vec2 y = z.zw;
    vec2 xsqr = sqr(x);
    vec2 ysqr = sqr(y);
    return vec4(
    mult(x, add(xsqr, mult(-3.0, ysqr))),
    mult(y, add(mult(3.0, xsqr), -ysqr))
    );
}

float quad(float a) {
    float asqr = sqr(a);
    return asqr*asqr;
}
vec2 cquad(vec2 z) {
    float xsqr = z.x*z.x;
    float ysqr = z.y*z.y;
    return vec2(
    xsqr*(xsqr - 3.0*ysqr) - ysqr*(3.0*xsqr - ysqr),
    4.0*z.x*z.y*(xsqr - ysqr)
    );
}
vec4 cquad(vec4 z) {
    vec2 x = z.xy;
    vec2 y = z.zw;
    vec2 xsqr = sqr(x);
    vec2 ysqr = sqr(y);
    return vec4(
    add(mult(xsqr, add(xsqr, mult(-3.0, ysqr))), -mult(ysqr, add(mult(3.0, xsqr), -ysqr))),
    mult(4.0, mult(x, mult(y, add(xsqr, -ysqr))))
    );
}

float quint(float a) {
    return quad(a)*a;
}
vec2 cquint(vec2 z) {
    float xsqr = z.x*z.x;
    float ysqr = z.y*z.y;
    return vec2(
    z.x*xsqr*(xsqr - 3.0*ysqr) - z.x*ysqr*(7.0*xsqr - 5.0*ysqr),
    xsqr*z.y*(5.0*xsqr - 7.0*ysqr) - z.y*ysqr*(3.0*xsqr - ysqr)
    );
}
vec4 cquint(vec4 z) {
    vec2 x = z.xy;
    vec2 y = z.zw;
    vec2 xsqr = sqr(x);
    vec2 ysqr = sqr(y);
    return vec4(
    add(mult(x, mult(xsqr, add(xsqr, mult(-3.0, ysqr)))), -mult(x, mult(ysqr, add(mult(7.0, xsqr), mult(-5.0, ysqr))))),
    add(mult(xsqr, mult(y, add(mult(5.0, xsqr), mult(-7.0, ysqr)))), -mult(y, mult(ysqr, add(mult(3.0, xsqr), -ysqr))))
    );
}

float cmodsqr(vec2 z) {
    return dot(z, z);
}
vec2 cmodsqr(vec4 z) {
    return add(sqr(z.xy), sqr(z.zw));
}

float cmod(vec2 z) {
    return sqrt(dot(z, z));
}
vec2 cmod(vec4 z) {
    return sqrtd(cmodsqr(z));
}
float cmod2(vec2 z) {
    float a = z.y/z.x;
    return z.x*sqrt(1.0 + a*a);
}
vec2 cmod2(vec4 z) {
    vec2 a = div(z.zw, z.xy);
    return mult(z.xy, sqrtd(ONE + sqr(a)));
}

vec2 cinv(vec2 w) {
    return conj(w)/dot(w, w);
}
vec4 cinv(vec4 w) {
    vec4 cw = conj(w);
    vec2 modsqrw = cmodsqr(w);
    return vec4(
        div(cw.xy, modsqrw),
        div(cw.zw, modsqrw)
    );
}

float carg(vec2 w) {
    return atan(w.y, w.x);
}
float carg(vec4 w) {
    return atan(w.z, w.x);
}

vec2 cdiv(float a, vec2 w) {
    return a*conj(w)/dot(w, w);
}
vec2 cdiv(vec2 z, vec2 w) {
    vec2 u = cmult(z, conj(w));
    return u/dot(w, w);
}
vec4 cdiv(float a, vec4 w) {
    vec4 u = cmult(a, conj(w));
    vec2 s = cmodsqr(w);
    return vec4(div(u.xy, s), div(u.zw, s));
}
vec4 cdiv(vec4 z, float a) {
    vec2 b = vec2(a, 0.0);
    return vec4(div(z.xy, b), div(z.zw, b));
}
vec4 cdiv(vec4 z, vec4 w) {
    vec2 s1 = cmodsqr(w);
    vec4 s2 = cmult(z, conj(w));
    vec2 p = div(s2.xy, s1);
    vec2 q = div(s2.zw, s1);
    return vec4(p, q);
}
vec4 cdiv(vec4 z, vec2 w) { return cdiv(z, _double(w)); }
vec4 cdiv(vec2 z, vec4 w) { return cdiv(_double(z), w); }

vec2 csqrt(vec2 z) {
    float p = sqrt(cmod(z));
    float phi = atan(z.y, z.x) / 2.0;
    return p*vec2(cos(phi), sin(phi));
}
vec4 csqrt(vec4 z) {
    vec2 p = sqrtd(cmod(z));
    vec2 t = 0.5*atan2(z.zw, z.xy);
    vec2 sint, cost;
    sincosd(t, sint, cost);
    return cmult(complex(p), vec4(cost, sint));
}

vec2 cexp(vec2 w) {
    if (w.y == 0.0) return _double(exp(w.x));
    else return exp(w.x)*vec2(cos(w.y), sin(w.y));
}
vec4 cexp(vec4 w) {
    vec2 t = expd(w.xy);
    if (w.zw == ZERO) return vec4(t, ZERO);
    else {
        vec2 siny, cosy;
        sincosd(w.zw, siny, cosy);
        return cmult(complex(t), vec4(cosy, siny));
    }
}

vec2 clog(vec2 z) {
    if (z == ZERO) return vec2(-1e30, 0.0);
    float rsqr = cmodsqr(z);
    float theta;
    if (z.y == 0.0) {
        if (z.x > 0.0) theta = 0.0;
        else theta = pi;
    }
    else theta = atan(z.y, z.x);
    return vec2(0.5*log(rsqr), theta);
}
vec4 clog(vec4 z) {
    vec2 rsqr = cmodsqr(z);
    vec2 theta;
    if (z.zw == ZERO) {
        if (gtd(z.xy, 0.0)) theta = ZERO;
        else theta = _pi;
    }
    else theta = atan2(z.zw, z.xy);
    return vec4(
        0.5*logd(rsqr),
        theta
    );
}

vec2 cpow(float x, vec2 s) {

    float theta;
    if (x == 0.0) return ZERO;
    else if (x > 0.0) theta = 0.0;
    else theta = pi;

    float lnx = log(abs(x));

    float c = exp(s.x*lnx - s.y*theta);
    float f = s.x*theta + s.y*lnx;
    return c*vec2(cos(f), sin(f));

}
vec4 cpow(vec2 x, vec4 s) {

    vec2 theta;
    if (x == ZERO) return vec4(ZERO, ZERO);
    else if (gtd(x, 0.0)) theta = ZERO;
    else theta = _pi;

    vec2 lnx = logd(absd(x));

    vec2 c, f, sinf, cosf;
    if (s.zw == ZERO) {
        if (theta == ZERO) return vec4(powd(x, s.xy), ZERO);
        c = expd(mult(s.xy, lnx));
        f = mult(s.xy, theta);
    }
    else if (s.xy == ZERO) {
        c = expd(-mult(s.zw, theta));
        f = mult(s.zw, lnx);
    }
    else {
        c = expd(add(mult(s.xy, lnx), -mult(s.zw, theta)));
        f = add(mult(s.xy, theta), mult(s.zw, lnx));
    }

    sincosd(f, sinf, cosf);
    return vec4(
        mult(c, cosf),
        mult(c, sinf)
    );

}

vec2 cpow(vec2 z, float p) {
    if (p == 2.0) return csqr(z);
    else if (p == 3.0) return ccube(z);
    else if (p == 4.0) return cquad(z);
    else if (p == 5.0) return cquint(z);
    else return cexp(p*clog(z));
}

vec2 cpow(vec2 z, vec2 s) {
    if (s.y == 0.0) return cpow(z, s.x);
    return cexp(cmult(s, clog(z)));
}
vec4 cpow(vec4 z, vec4 s) {
    if (s.zw == ZERO) {
        if (s.xy == vec2(2.0, 0.0)) return csqr(z);
        else if (s.xy == vec2(3.0, 0.0)) return ccube(z);
        else if (s.xy == vec2(4.0, 0.0)) return cquad(z);
        else if (s.xy == vec2(5.0, 0.0)) return cquint(z);
        else return cexp(cmult(complex(s.xy), clog(z)));
    }
    return cexp(cmult(s, clog(z)));
}
vec4 cpow(vec4 z, vec2 s) {
    return cpow(z, _double(s));
}

float _m(float _a, float _b) {
    return (_a*_b) * Sp;
}

vec2 _cmult(vec2 _a, vec2 _b) {
    return vec2(_m(_a.x, _b.x) - _m(_a.y, _b.y), _m(_a.x, _b.y) + _m(_b.x, _a.y));
}

vec2 csin_series(vec2 z) {
    vec2 w = z;
    vec2 zPow = z;
    vec2 zSqr = csqr(z);
    float s = 1.0;
    float a = 1.0;
    for (int i = 3; i <= 9; i += 2) {
        s *= -1.0;
        a *= float(i)*float(i - 1);
        zPow = cmult(zPow, zSqr);
        w += (s/a) * zPow;
    }
    return w;
}
vec2 ccos_series(vec2 z) {
    vec2 _w = ONE*Sn;
    vec2 _zPow = ONE*Sn;
    vec2 zSqr = csqr(z);
    float s = 1.0;
    float a = 1.0;
    for (int i = 2; i <= 8; i += 2) {
        s *= -1.0;
        a *= float(i)*float(i - 1);
        _zPow = cmult(_zPow, zSqr);
        _w += (s/a) * _zPow;
    }
    return _w*Sp;
}

vec2 csin(vec2 z) {
    float t1 = exp(z.y);
    float t2 = 1.0/t1;
    return 0.5*vec2(
        sin(z.x)*(t1 + t2),
        cos(z.x)*(t1 - t2)
    );
}
vec4 csin(vec4 z) {
    vec2 t1 = expd(z.zw);
    vec2 t2 = div(ONE, t1);

    vec2 sinx, cosx;
    sincosd(z.xy, sinx, cosx);

    return 0.5*vec4(
        mult(sinx, add(t1, t2)),
        mult(cosx, add(t1, -t2))
    );
}

vec2 ccos(vec2 z) {
    float t1 = exp(z.y);
    float t2 = 1.0/t1;
    return 0.5*vec2(
        cos(z.x)*(t1 + t2),
        -sin(z.x)*(t1 - t2)
    );
}
vec4 ccos(vec4 z) {
    vec2 t1 = expd(z.zw);
    vec2 t2 = div(ONE, t1);

    vec2 sinx, cosx;
    sincosd(z.xy, sinx, cosx);

    return 0.5*vec4(
        mult(cosx, add(t1, t2)),
        mult(-sinx, add(t1, -t2))
    );
}

vec2 ctan(vec2 z) { return cdiv(csin(z), ccos(z)); }
vec4 ctan(vec4 z) { return cdiv(csin(z), ccos(z)); }

vec2 ccsc(vec2 z) { return cinv(csin(z)); }
vec4 ccsc(vec4 z) { return cinv(csin(z)); }

vec2 csec(vec2 z) { return cinv(ccos(z)); }
vec4 csec(vec4 z) { return cinv(ccos(z)); }

vec2 ccot(vec2 z) { return cinv(ctan(z)); }
vec4 ccot(vec4 z) { return cinv(ctan(z)); }

vec2 csinh(vec2 z) {
    float t1 = exp(z.y);
    float t2 = 1.0/t1;
    return 0.5*vec2(
        cos(z.y)*(t1 - t2),
        sin(z.y)*(t1 + t2)
    );
}

vec2 ccosh(vec2 z) {
    float t1 = exp(z.y);
    float t2 = 1.0/t1;
    return 0.5*vec2(
        cos(z.y)*(t1 + t2),
        sin(z.y)*(t1 - t2)
    );
}

vec2 ctanh(vec2 z) { return cdiv(csinh(z), ccosh(z)); }

vec2 ccsch(vec2 z) { return cinv(csinh(z)); }

vec2 csech(vec2 z) { return cinv(ccosh(z)); }

vec2 ccoth(vec2 z) { return cinv(ctanh(z)); }

vec2 boxfold(vec2 z) {
    vec2 w = z;
    if (z.x < -1.0) { w.x = -2.0 - z.x; }
    else if (z.x > 1.0) { w.x = 2.0 - z.x; }
    if (z.y < -1.0) { w.y = -2.0 - z.y; }
    else if (z.y > 1.0) { w.y = 2.0 - z.y; }
    return w;
}
vec4 boxfold(vec4 z) {
    vec2 x = z.xy;
    vec2 y = z.zw;
    vec2 u = x;
    vec2 v = y;
    if (x.x < -1.0) { u = add(-2.0*ONE, -x); }
    else if (x.x > 1.0) { u = add(2.0*ONE, -x); }
    if (y.x < -1.0) { v = add(-2.0*ONE, -y); }
    else if (y.x > 1.0) { v = add(2.0*ONE, -y); }
    return vec4(u, v);
}

vec2 ballfold(vec2 z) {
    float modsqrz = cmodsqr(z);
    float coef = 1.0;
    if (modsqrz < 0.25) { coef = 4.0; }
    else if (modsqrz > 0.25 && modsqrz < 1.0) { coef = 1.0/modsqrz; }
    return coef*z;
}
vec4 ballfold(vec4 z) {
    vec2 modsqrz = cmodsqr(z);
    vec4 w = z;
    if (modsqrz.x < 0.25) { w = cmult(4.0, z); }
    else if (modsqrz.x > 0.25 && modsqrz.x < 1.0) { w = cdiv(z, complex(modsqrz)); }
    return w;
}


float triwave(float x) {
    return 1.0 - 4.0*abs(0.5 - fract(1.0/(2.0*pi)*x + 0.25));
}

float sqrwave(float x) {
    return sign(sin(x));
}

vec2 polygon(float theta, float n) {
    float m = 0.5*n*(theta/pi + 1.0);
    float k = ceil(m) - 1.0;
    float frac = fract(m);
    float phi1 = 2.0*k*pi/n;
    float phi2 = 2.0*(k + 1.0)*pi/n;
    vec2 root1 = vec2(cos(phi1), sin(phi1));
    vec2 root2 = vec2(cos(phi2), sin(phi2));
    return -(frac*root1 + (1.0 - frac)*root2);
}

vec2 _sqr(vec2 _a) {
    return vec2(_m(_a.x, _a.x) - _m(_a.y, _a.y), 2.0*_m(_a.x, _a.y));
}

float _cmod(vec2 _a) {
    return sqrt(_m(_a.x, _a.x) + _m(_a.y, _a.y)) * Sh;
}

float _cmodsqr(vec2 _a) {
    return _m(_a.x, _a.x) + _m(_a.y, _a.y);
}

float wrapb(float a, float w) {
    float b = a;
    if (a > w) { b = 2.0*w - a; }
    else if (a < -w) { b = -2.0*w - a; }
    return b;
}
vec2 wrapb(vec2 a, float w) {
    vec2 b = a;
    if (a.x > w || (a.x == w && a.y > w) ) { b = add(vec2(2.0*w, 0.0), -a); }
    else if (a.x < -w || (a.x == -w && a.y < -w)) { b = add(vec2(-2.0*w, 0.0), -a); }
    return b;
}


vec2 wrapbox(vec2 a, float h, float w) {
    return vec2(wrapb(a.x, w), wrapb(a.y, h));
}
vec4 wrapbox(vec4 z, float h, float w) {
    return vec4(wrapb(z.xy, w), wrapb(z.zw, h));
}

vec2 wrapcirc(vec2 p, float r) {
    vec2 q = p;
    float l = length(p);
    vec2 u = p/l;
    if (l > r) q = 2.0*r*u - p;
    return q;
}
vec4 wrapcirc(vec4 z, float r) {
    vec4 w = z;
    vec2 L = cmod(z);
    vec4 q = cdiv(z, complex(L));
    if (L.x > r || (L.x == r && L.y > r)) {
        w = cadd(2.0*cmult(r, q), -z);
    }
    return w;
}

vec2 rotate(vec2 a, float phi) {
    return vec2(a.x*cos(phi) - a.y*sin(phi), a.x*sin(phi) + a.y*cos(phi));
}
vec4 rotate(vec4 z, float phi) {
    return cmult(z, vec2(cos(phi), sin(phi)));
}
vec2 rotate(vec2 a, float sinPhi, float cosPhi) {
    return vec2(a.x*cosPhi - a.y*sinPhi, a.x*sinPhi + a.y*cosPhi);
}

vec2 circinv(vec2 p, float r) {
    vec2 q = p;
    float l = length(p);
    if (l < r) q = p*r*r/(l*l);
    return q;
}
vec4 circinv(vec4 z, float r) {
    vec4 w = z;
    vec2 L2 = cmodsqr(z);
    float r2 = r*r;
    vec2 M = div(vec2(r2, 0.0), L2);
    if (L2.x < r2 || (L2.x == r2 && L2.y < r2)) w = cmult(complex(M), z);
    return w;
}

vec2 linearpull(vec2 p, float h, float w) {
    vec2 q = p;
    if (abs(q.x) > w && abs(q.y) > h) {
        h *= 2.0; w *= 2.0;
        if (q.x < 0.0) { q.x += w; }
        else { q.x -= w; }
        if (q.y < 0.0) { q.y += h; }
        else { q.y -= h; }
    }
    return q;
}
vec4 linearpull(vec4 z, float h, float width) {
    vec4 w = z;
    vec2 absu = absd(z.xy);
    vec2 absv = absd(z.zw);
    if ((absu.x > width || (absu.x == width && absu.y > width)) && (absv.x > h || (absv.x == h && absv.y > h))) {
        h *= 2.0; width *= 2.0;
        if (w.x < 0.0 || (w.x == 0.0 && w.y < 0.0)) { w.xy = add(w.xy, vec2(width, 0.0)); }
        else { w.xy = add(w.xy, vec2(-width, 0.0)); }
        if (w.z < 0.0 || (w.z == 0.0 && w.w < 0.0)) { w.zw = add(w.zw, vec2(h, 0.0)); }
        else { w.zw = add(w.zw, vec2(-h, 0.0)); }
    }
    return w;
}

vec2 nonlinearpull(vec2 p, float h, float w) {
    vec2 q = p;
    if (abs(q.x) > w && abs(q.y) > h) {
        h *= 2.0; w *= 2.0;
        if (q.x > 0.0) { q.x -= w*floor(q.x/w); }
        else { q.x -= w*ceil(q.x/w); }
        if (q.y > 0.0) { q.y -= h*floor(q.y/h); }
        else { q.y -= h*ceil(q.y/h); }
    }
    return q;
}
vec4 nonlinearpull(vec4 z, float h, float width) {
    vec4 w = z;
    if (absd(z.xy).x > width && absd(z.zw).x > h) {
        h *= 2.0; width *= 2.0;
        if (w.x > 0.0 || (w.x == 0.0 && w.y > 0.0)) { w.xy = add(w.xy, -vec2(width*floor(div(w.xy, vec2(width, 0.0)).x), 0.0)); }
        else { w.xy = add(w.xy, -vec2(width*ceil(div(w.xy, vec2(width, 0.0)).x), 0.0)); }
        if (w.z > 0.0 || (w.z == 0.0 && w.w > 0.0)) { w.zw = add(w.zw, -vec2(h*floor(div(w.zw, vec2(h, 0.0)).x), 0.0)); }
        else { w.zw = add(w.zw, -vec2(h*ceil(div(w.zw, vec2(h, 0.0)).x), 0.0)); }
    }
    return w;
}





vec2 rect(vec2 z) {
    float cost = cos(z.y);
    //float sint = 1.0 - cost*cost;
    float sint = sin(z.y);
    return z.x*vec2(cost, sint);
}
vec4 rect(vec4 z) {
    vec2 sint, cost;
    sincosd(z.zw, sint, cost);
    return vec4(
        mult(z.xy, cost),
        mult(z.xy, sint)
    );
}

vec2 polar(vec2 z) { return vec2(cmod(z), atan(z.y, z.x)); }
vec4 polar(vec4 z) { return vec4(cmod(z), atan2(z.zw, z.xy)); }

vec2 mod_theta(vec2 z) { return vec2(z.x, mod(z.y + pi, 2.0*pi) - pi); }
vec4 mod_theta(vec4 z) { return vec4(z.xy, add(modd(add(z.zw, _pi), 2.0*pi), -_pi)); }

vec2 cadd_polar(vec2 z, vec2 w) {

    float cost = cos(w.y - z.y);
    float sint = sin(w.y - z.y);
    return vec2(
        sqrt(z.x*z.x + w.x*w.x + 2.0*z.x*w.x*cost),
        z.y + atan(w.x*sint, z.x + w.x*cost)
    );

}
vec4 cadd_polar(vec4 z, vec4 w) {

    vec2 cost, sint;
    sincosd(add(w.zw, -z.zw), sint, cost);
    return vec4(
        sqrtd(add(add(sqr(z.xy), sqr(w.xy)), 2.0*mult(z.xy, mult(w.xy, cost)))),
        add(z.zw, atan2(mult(w.xy, sint), add(z.xy, mult(w.xy, cost))))
    );

}

vec2 csqr_polar(vec2 z) { return vec2(z.x*z.x, 2.0*z.y); }
vec4 csqr_polar(vec4 z) { return vec4(sqr(z.xy), 2.0*z.zw); }

vec2 cmult_polar(vec2 z, vec2 w) { return vec2(z.x*w.x, z.y + w.y); }
vec4 cmult_polar(vec4 z, vec4 w) { return vec4(mult(z.xy, w.xy), add(z.zw, w.zw)); }

vec2 cpow_polar(vec2 z, vec2 s) {

    if (s.y == 0.0 && s.x == 2.0) return csqr_polar(z);
    else {
        float lnr = log(z.x);
        return vec2(
            exp(s.x*lnr - s.y*z.y),
            s.x*z.y + s.y*lnr
        );
    }

}
vec4 cpow_polar(vec4 z, vec4 s) {

    if (s.zw == ZERO && s.xy == vec2(2.0, 0.0)) return csqr_polar(z);
    else {
        vec2 lnr = logd(z.xy);
        return vec4(
            expd(add(mult(s.xy, lnr), -mult(s.zw, z.zw))),
            add(mult(s.xy, z.zw), mult(s.zw, lnr))
        );
    }

}

vec2 cinv_polar(vec2 z) { return vec2(1.0/z.x, -z.y); }
vec4 cinv_polar(vec4 z) { return vec4(inv(z.xy), -z.zw); }






vec2 testshape(vec2 z, vec2 c) { return csqr(z) + c; }
vec4 testshape(vec4 z, vec4 c) { return cmult_polar(c, cpow_polar(z, vec4(p1.x, 0.0, p1.y, 0.0))); }

vec2 mandelbrot(vec2 z, vec2 c) { return csqr(z) + c; }
vec4 mandelbrot(vec4 z, vec4 c) { return cadd(csqr(z), c); }
vec2 mandelbrot_delta1(vec2 alpha, vec2 z1) {
    vec2 powz1 = z1;
    for (int i = 1; i < int(power) - 1; i++) {
        powz1 = cmult(powz1, z1);
    }
    return power*cmult(powz1, alpha) + ONE;
}
vec2 mandelbrot_delta1(vec2 alpha, vec4 z1) {
    return mandelbrot_delta1(alpha, z1.xz);
}
vec2 mandelbrot_delta2(vec2 beta, vec2 alpha, vec2 z1) {
    vec2 p = ONE;
    for (int i = 0; i < int(power) - 2; i++) { p = cmult(p, z1); }
    vec2 q = cmult(p, z1);
    return power*((power - 1.0)*cmult(p, csqr(alpha)) + cmult(q, beta));
}
vec2 mandelbrot_delta2(vec2 beta, vec2 alpha, vec4 z1) {
    return mandelbrot_delta2(beta, alpha, z1.xz);
}

vec2 mandelbrot_cubic(vec2 z, vec2 c) { return ccube(z) + c; }
vec4 mandelbrot_cubic(vec4 z, vec4 c) { return cadd(ccube(z), c); }

vec2 mandelbrot_quartic(vec2 z, vec2 c) { return cquad(z) + c; }
vec4 mandelbrot_quartic(vec4 z, vec4 c) { return cadd(cquad(z), c); }

vec2 mandelbrot_quintic(vec2 z, vec2 c) { return cquint(z) + c; }
vec4 mandelbrot_quintic(vec4 z, vec4 c) { return cadd(cquint(z), c); }

vec2 mandelbrot_power(vec2 z, vec2 c) { return cpow(z, p1) + c; }
vec4 mandelbrot_power(vec4 z, vec4 c) { return cadd(cpow(z, p1), c); }

vec2 clover(vec2 z, vec2 c) {
    vec2 q = cpow(z, p1);
    return cmult(c, q + cinv(q));
//    vec2 q = cpow_polar(z, p1);
//    return mod_theta(cmult_polar(c, cadd_polar(q, cinv_polar(q))));
}
vec4 clover(vec4 z, vec4 c) {
    vec4 q = cpow(z, p1);
    return cmult(c, cadd(q, cinv(q)));
//    vec4 q = cpow_polar(z, complexd(p1));
//    return mod_theta(cmult_polar(c, cadd_polar(q, cinv_polar(q))));
}

vec2 mandelbox(vec2 z, vec2 c) {
    return cmult(p1, ballfold(cmult(p2, boxfold(z)))) - c;
}
vec4 mandelbox(vec4 z, vec4 c) {
    return cadd(cmult(p1, ballfold(cmult(p2, boxfold(z)))), -c);
}

vec2 kali(vec2 z, vec2 c) {
    vec2 w = abs(z);
    return w/cmodsqr(w) + c;
}
vec4 kali(vec4 z, vec4 c) {
    vec4 w = vec4(absd(z.xy), absd(z.zw));
    return cadd(cdiv(w, complex(cmodsqr(w))), c);
}

vec2 kali_square(vec2 z, vec2 c) { return abs(z)/(z.x*z.y) + c; }

vec2 mandelbar(vec2 z, vec2 c) { return csqr(conj(z)) + c; }

vec2 burning_ship(vec2 z, vec2 c) { return csqr(abs(z)) + c; }
vec4 burning_ship(vec4 z, vec4 c) { return cadd(csqr(vec4(absd(z.xy), absd(z.zw))), c); }

vec2 sine1(vec2 z, vec2 c) { return csin(z) + c; }
vec4 sine1(vec4 z, vec4 c) { return cadd(csin(z), c); }

vec2 sine2(vec2 z, vec2 c) { return csin(cdiv(csqr(z) + p1, c)); }
vec4 sine2(vec4 z, vec4 c) { return csin(cdiv(cadd(csqr(z), p1), c)); }

vec2 horseshoe_crab(vec2 z, vec2 c) { return csin(cdiv(p1, z) + cdiv(z, c)); }
vec4 horseshoe_crab(vec4 z, vec4 c) { return csin(cadd(cdiv(p1, z), cdiv(z, c))); }

void kleinian_init(inout vec2 z, vec2 c) {
    vec2 q = z - c;
    float h = atan(q.y, q.x);
    z = c + vec2(cos(h), sin(h))/cmod(q);
}
vec2 kleinian(vec2 z, vec2 c) {
    vec2 w = z;
    w.x = mod(z.x + 2.0*float(maxIter) - 1.0, 2.0) - 1.0;
    if (z.y < p1.x/2.0) {
        return cdiv(p1.x*w - I, cmult(-I, w));
    }
    else {
        return cdiv(I, cmult(I, w) + vec2(p1.x, 0.0));
    }
}
bool kleinian_exit(inout float modsqrz, vec2 z) {
    modsqrz = cmodsqr(z);
    if (modsqrz > R*R || z.y < 0.0 || z.y > p1.x) { return true; }
    return false;
}

vec2 newton1(vec2 z, vec2 c) { return z - cdiv(cquad(z) - ONE, 4.0*ccube(z)); }

vec2 nova1(vec2 z, vec2 c) {
    vec2 zsqr = csqr(z);
    return z - cmult(p1, cdiv(cmult(z, zsqr) - ONE, 3.0*zsqr)) + c;
}
vec4 nova1(vec4 z, vec4 c) {
    vec4 zsqr = csqr(z);
    return cadd(cadd(z, -cmult(vec4(p1.x, 0.0, p1.y, 0.0), cdiv(cadd(cmult(z, zsqr), -vec4(ONE, ZERO)), cmult(vec4(3.0*ONE, ZERO), zsqr)))), c);
}

vec2 nova2(vec2 z, vec2 c) {
    vec2 zSqr = csqr(z);
    return z - cdiv(csin(zSqr) - z, 2.0*cmult(z, ccos(zSqr)) - ONE) + c;
}
vec4 nova2(vec4 z, vec4 c) {
    vec4 zsqr = csqr(z);
    return cadd(cadd(z, -cdiv(cadd(csin(zsqr), -z), cadd(2.0*cmult(z, ccos(zsqr)), -1.0))), c);
}

vec2 collatz(vec2 z, vec2 c) {
    return 0.25*(2.0*ONE + 7.0*z - cmult((2.0*ONE + 5.0*z), ccos(pi*z))) + c;
}
vec4 collatz(vec4 z, vec4 c) {
    return cadd(0.25*cadd(cadd(2.0, cmult(7.0, z)), -cmult(cadd(2.0, cmult(5.0, z)), ccos(cmult(pi, z)))), c);
}

vec2 mandelex(vec2 z, vec2 c) {
    vec2 w = linearpull(z, p4.x, p4.x);
    if (abs(c.x) < 2.0 && abs(c.y) < 2.0) { w = nonlinearpull(w, 2.0, 2.0); }
    return linearpull(rotate(circinv(wrapbox(w, 0.5, 0.5), p2.x)*p3.x, p1.x/180.0*3.1415926) + c, p4.x, p4.x);
}
vec4 mandelex(vec4 z, vec4 c) {
    vec4 w = linearpull(z, p4.x, p4.x);
    if (abs(c.x) < 2.0 && abs(c.z) < 2.0) { w = nonlinearpull(w, 2.0, 2.0); }
    return linearpull(cadd(rotate(cmult(vec4(p3.x, 0.0, 0.0, 0.0), circinv(wrapbox(w, 0.5, 0.5), p2.x)), p1.x/180.0*3.1415926), c), p4.x, p4.x);
}

vec2 binet(vec2 z, vec2 c) {
    return (cpow(phi, z) - cpow(-1.0/phi, z))/sqrt(5.0) + c;
}
vec4 binet(vec4 z, vec4 c) {
    return cadd(cdiv(cadd(cpow(vec2(phi, 0.0), z), -cpow(vec2(-1.0/phi, 0.0), z)), sqrt(5.0)), c);
}




float divergence(vec2 z, vec2 z1, out float modz1, bool textureIn) {

    float modz = cmod2(z);
    modz1 = cmod2(z1);
    float div;
    //    float div = log(modz)/log(modz1);

    if (isSpecial(z.x) || isSpecial(z.y))   div = log(1e38)/log(modz1);
    else if (isSpecial(modz))               div = 0.5*log(1e38)/log(modz1);
    else if (textureIn)                     div = 0.0;
    else                                    div = log(modz)/log(modz1);

    return div;

}
float divergence(vec4 z, vec4 z1, out vec2 modz1, bool textureIn) {

    vec2 modz = cmod(z);
    modz1 = cmod(z1);
    float div;
    //    float div = log(modz)/log(modz1);

    if (isSpecial(z.x) || isSpecial(z.z))   div = log(1e38)/log(modz1.x);
    else if (isSpecial(modz.x))             div = 0.5*log(1e38)/log(modz1.x);
    else if (textureIn)                     div = 0.0;
    else                                    div = log(modz.x)/log(modz1.x);

    return div;

}

float iteration_final(uint n) {
    float j = float(n);
    return log(log(j + 1.0) + 1.0);
}

void exp_smoothing_loop(inout float sum, float modsqrz) {
    sum += exp(-modsqrz);
}
void exp_smoothing_loop(inout vec2 sum, vec2 modsqrz) {
    sum.x += exp(-modsqrz.x);
}
float exp_smoothing_final(float sum) {
    return log(log(sum + 1.0) + 1.0);
}
float exp_smoothing_final(vec2 sum) {
    return exp_smoothing_final(sum.x);
}

float escape_smooth_final(uint n, vec2 z, vec2 z1, bool textureIn) {
    float modz1;
    float div = divergence(z, z1, modz1, textureIn);
    float i = float(n) - log(log(modz1)/log(R))/log(div);
    return log(log(i + 1.0) + 1.0);
}
float escape_smooth_final(uint n, vec4 z, vec4 z1, bool textureIn) {
    vec2 modz1;
    float div = divergence(z, z1, modz1, textureIn);
    float i = float(n) - log(log(modz1.x)/log(R))/log(div);
    return log(log(i + 1.0) + 1.0);
}

float dist_estim_final(float modsqrz, vec2 alpha) {
    return sqrt(modsqrz)*0.5*log(modsqrz)/cmod(alpha);
}
float dist_estim_final(vec2 modsqrz, vec2 alpha) {
    return sqrt(modsqrz.x)*0.5*log(modsqrz.x)/cmod(alpha);
}

void orbit_trap_minx_loop(inout float minx, vec2 z) {
    float dist = abs(z.x);
    if (dist < minx) { minx = dist; }
}
void orbit_trap_minx_loop(inout float minx, vec4 z) {
    float dist = abs(z.x);
    if (dist < minx) { minx = dist; }
}

float normal_map1_final(vec2 z, vec2 alpha) {
    vec2 u = cdiv(z, alpha);
    return atan(u.y, u.x)/(2.0*pi) + 1.0;
}
float normal_map1_final(vec4 z, vec2 alpha) {
    return normal_map1_final(z.xz, alpha);
}

float normal_map2_final(float modsqrz, vec2 z, vec2 alpha, vec2 beta) {
    float logmodz = 0.5*log(modsqrz);
    vec2 u = cmult(cmult(z, alpha), (logmodz + 1.0)*conj(csqr(alpha)) - logmodz*conj(cmult(z, beta)));
    return atan(u.y, u.x)/(2.0*pi) + 1.0;
}
float normal_map2_final(vec2 modsqrz, vec4 z, vec2 alpha, vec2 beta) {
    return normal_map2_final(modsqrz.x, z.xz, alpha, beta);
}

float avg_final(float sum, float sum1, uint n, vec2 z, vec2 z1, bool textureIn) {

    sum /= float(n + 1u);
    sum1 /= float(n);

    float modz1;
    float div = divergence(z, z1, modz1, textureIn);

    float r = -log(log(modz1)/log(R))/log(div);
    float s = (1.0 - r)*sum1 + r*sum;
    return s;

}
float avg_final(vec2 sum, vec2 sum1, uint n, vec4 z, vec4 z1, bool textureIn) {

    sum.x /= float(n + 1u);
    sum1.x /= float(n);

    vec2 modz1;
    float div = divergence(z, z1, modz1, textureIn);

    float r = -log(log(modz1.x)/log(R))/log(div);
    float s = (1.0 - r)*sum1.x + r*sum.x;
    return s;

}

void stripe_avg_loop(inout float sum, inout float sum1, vec2 z) {
    sum1 = sum;
    float argz = atan(z.y, z.x);
    sum += pow(0.5*(sin(q1*argz) + 1.0), q2);
}
void stripe_avg_loop(inout vec2 sum, inout vec2 sum1, vec4 z) {
    sum1 = sum;
    float argz = atan(z.z, z.x);
    sum.x += pow(0.5*(sin(q1*argz) + 1.0), q2);
}
float stripe_avg_final(float sum, float sum1, uint n, float il, float llr, vec2 z1) {
    sum /= float(n + 1u);
    sum1 /= float(n);
    float r = 1.0 + il*llr - il*log(0.5*log(cmodsqr(z1)));
    float s = (1.0 - r)*sum1 + r*sum;
    return s;
}
float stripe_avg_final(vec2 sum, vec2 sum1, uint n, float il, float llr, vec4 z1) {
    sum.x /= float(n + 1u);
    sum1.x /= float(n);
    float r = 1.0 + il*llr - il*log(0.5*log(cmodsqr(z1.xz)));
    float s = (1.0 - r)*sum1.x + r*sum.x;
    return s;
}

void triangle_ineq_avg_int_loop(inout float sum, inout float sum1, uint n, float modc, vec2 z1, vec2 z2) {
    if (n > 2u) {
        sum1 = sum;
        float modsqrz2 = cmodsqr(z2);
        float modpowz2 = modsqrz2;
        for (int i = 2; i < int(power); i += 2) {
            if (int(power) - i >= 2) { modpowz2 *= modsqrz2; }
            else { modpowz2 *= sqrt(modsqrz2); }
        }
        float m = abs(modpowz2 - modc);
        float M = modpowz2 + modc;
        float p = cmod(z1) - m;
        float q = M - m;
        sum += p/q;
    }
}
void triangle_ineq_avg_int_loop(inout vec2 sum, inout vec2 sum1, uint n, vec2 modc, vec4 z1, vec4 z2) {
    if (n > 2u) {
        sum1 = sum;
        vec2 modsqrz2 = cmodsqr(z2);
        vec2 modpowz2 = modsqrz2;
        for (int i = 2; i < int(power); i += 2) {
            if (int(power) - i >= 2) { modpowz2 = mult(modpowz2, modsqrz2); }
            else { modpowz2 = mult(modpowz2, sqrtd(modsqrz2)); }
        }
        vec2 m = absd(add(modpowz2, -modc));
        vec2 M = add(modpowz2, modc);
        vec2 p = add(cmod(z1), -m);
        vec2 q = add(M, -m);
        sum = add(sum, div(p, q));
    }
}
void triangle_ineq_avg_float_loop(inout float sum, inout float sum1, uint n, float modc, vec2 z1, vec2 z2) {
    if (n > 2u) {
        sum1 = sum;
        float modsqrz2 = cmodsqr(z2);
        float modpowz2 = pow(modsqrz2, 0.5*power);
        float m = abs(modpowz2 - modc);
        float M = modpowz2 + modc;
        float p = cmod(z1) - m;
        float q = M - m;
        sum += p/q;
    }
}
void triangle_ineq_avg_float_loop(inout vec2 sum, inout vec2 sum1, uint n, vec2 modc, vec4 z1, vec4 z2) {
    if (n > 2u) {
        sum1 = sum;
        vec2 modsqrz2 = cmodsqr(z2);
        vec2 modpowz2 = pow(modsqrz2.x, 0.5*power)*ONE;
        vec2 m = absd(add(modpowz2, -modc));
        vec2 M = add(modpowz2, modc);
        vec2 p = add(cmod(z1), -m);
        vec2 q = add(M, -m);
        sum = add(sum, div(p, q));
    }
}
float triangle_ineq_avg_final(float sum, float sum1, uint n, float il, float llr, vec2 z1) {
    sum /= float(n + 1u);
    sum1 /= float(n);
    float r = 1.0 + il*llr - il*log(0.5*log(cmodsqr(z1)));
    float s = (1.0 - r)*sum1 + r*sum;
    return s;
}
float triangle_ineq_avg_final(vec2 sum, vec2 sum1, uint n, float il, float llr, vec4 z1) {
    sum = mult(1.0/float(n + 1u), sum);
    sum1 = mult(1.0/float(n), sum1);
    float r = 1.0 + il*llr - il*log(0.5*log(cmodsqr(z1).x));
    float s = (1.0 - r)*sum1.x + r*sum.x;
    return s;
}

void curvature_avg_loop(inout float sum, inout float sum1, uint n, vec2 z, vec2 z1, vec2 z2) {
    if (n > 1u) {
        sum1 = sum;
        vec2 w = cdiv(z - z1, z1 - z2);
        sum += pow(abs(atan(w.y, w.x))/pi, q1);
    }
}
void curvature_avg_loop(inout vec2 sum, inout vec2 sum1, uint n, vec4 z, vec4 z1, vec4 z2) {
    if (n > 1u) {
        sum1.x = sum.x;
        vec2 w = cdiv(vec2(z.x - z1.x, z.z - z1.z), vec2(z1.x - z2.x, z1.z - z2.z));
        sum.x += pow(abs(atan(w.y, w.x))/pi, q1);
    }
}
float curvature_avg_final(float sum, float sum1, uint n, float il, float llr, vec2 z1) {
    sum /= float(n + 1u);
    sum1 /= float(n);
    float r = 1.0 + il*llr - il*log(0.5*log(cmodsqr(z1)));
    float s = (1.0 - r)*sum1 + r*sum;
    return s;
}
float curvature_avg_final(vec2 sum, vec2 sum1, uint n, float il, float llr, vec4 z1) {
    sum /= float(n + 1u);
    sum1 /= float(n);
    float r = 1.0 + il*llr - il*log(0.5*log(cmodsqr(z1.xz)));
    float s = (1.0 - r)*sum1.x + r*sum.x;
    return s;
}

void overlay_avg_loop(inout float sum, inout float sum1, vec2 z) {
    sum1 = sum;
    float argz = atan(z.y, z.x);
    sum += 0.5*(tan(q1*argz)/tan(q1*pi) + 1.0);
}
void overlay_avg_loop(inout vec2 sum, inout vec2 sum1, vec4 z) {
    sum1.x = sum.x;
    float argz = atan(z.z, z.x);
    sum.x += 0.5*(tan(q1*argz)/tan(q1*pi) + 1.0);
}
float overlay_avg_final(float sum, float sum1, uint n, float il, float llr, vec2 z1) {
    sum /= float(n);
    sum1 /= float(n - 1u);
    float r = il*llr - il*log(0.5*log(cmodsqr(z1)));
    float s = sum1 + (sum - sum1)*(r + 1.0);
    return s;
}
float overlay_avg_final(vec2 sum, vec2 sum1, uint n, float il, float llr, vec4 z1) {
    sum.x /= float(n);
    sum1.x /= float(n - 1u);
    float r = il*llr - il*log(0.5*log(cmodsqr(z1.xz)));
    float s = sum1.x + (sum.x - sum1.x)*(r + 1.0);
    return s;
}

void test_avg_loop(inout float sum, inout float sum1, uint n, vec2 z, vec2 z1, vec2 z2, vec2 z3, vec2 z4, vec2 c) {

//    sum1 = sum;
//    vec2 w = cmult(z, conj(z1));
//    sum += pow(0.5*(acos(w.x) + 1.0), q1);
//    sum += pow(0.5*(cos(w.x) + 1.0), q1);

//    float w = atan2(z)/atan2(z1);
//    sum += 0.5*(q1*sin(w) + 1.0);


    // angular momentum
//    vec2 v = z1 - z2;
//    float r = cmod(z);
//    float phi = atan2(v) - atan2(z);
//    float vperp = dot(cmult(I, z)/r, v);
//    float vang = vperp/r;
//    sum += vang;


    if (n > 3u) {
        sum1 = sum;
        vec2 v = z - z1;
        vec2 v1 = z1 - z2;
        vec2 v2 = z2 - z3;
        vec2 v3 = z3;
        vec2 w = rotate(cdiv(cmult(v, v2), cmult(v1, v3)), q1);
        sum += abs(atan2(w))/pi;
    }


}
void test_avg_loop(inout vec2 sum, inout vec2 sum1, uint n, vec4 z, vec4 z1, vec4 z2, vec4 z3, vec4 z4, vec4 c) {

    if (n > 3u) {
        sum1 = sum;
        vec2 v = z.xz - z1.xz;
        vec2 v1 = z1.xz - z2.xz;
        vec2 v2 = z2.xz - z3.xz;
        vec2 v3 = z3.xz;
        vec2 w = cdiv(cmult(v, v2), cmult(v1, v3));
        sum += abs(atan2(w))/pi;
    }

}




void julia_seed(inout vec2 z, vec2 c) {
    z = c;
}
void julia_seed(inout vec4 z, vec4 c) {
    z = c;
}
void julia_mode(inout vec2 z, inout vec2 c) {
    z = c;
    c = j;
}
void julia_mode(inout vec4 z, inout vec4 c) {
    z = c;
    c = _double(j);
}



bool escape(inout float modsqrz, vec2 z) {
    modsqrz = cmodsqr(z);
    if (modsqrz > R*R || isnan(modsqrz) || isinf(modsqrz)) { return true; }
    return false;
}
bool escape(inout vec2 modsqrz, vec4 z) {
    modsqrz = cmodsqr(z);
    if (modsqrz.x > R*R || isnan(modsqrz.x) || isinf(modsqrz.x)) { return true; }
    return false;
}

bool converge(inout float eps, vec2 z, vec2 z1) {
    eps = cmodsqr(z - z1);
    if (eps < 1e-8) { return true; }
    return false;
}
bool converge(inout float eps, vec4 z, vec4 z1) {
    eps = cmodsqr(cadd(z, -z1)).x;
    if (eps < 1e-8) { return true; }
    return false;
}




void main() {

    bool textureIn = false;

    // generalInit
    // seedInit
    // shapeInit
    // textureInit

    // c = mod(c - 2.0, 4.0) - 2.0;

    for (uint n = 0u; n <= maxIter; n++) {

        if (n == maxIter) {
            textureIn = true;
            // textureFinal
            colorParams.y = 1.0;
            break;
        }

        z4 = z3;
        z3 = z2;
        z2 = z1;
        z1 = z;

        // shapeLoop
        // textureLoop

        /* conditional */ {
            // textureFinal
            break;
        }

    }

    if (isinf(colorParams.x) || isnan(colorParams.x)) colorParams.x = 0.0;
    fragmentColor = floatBitsToUint(colorParams);

}

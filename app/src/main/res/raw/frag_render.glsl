#version 300 es

precision highp float;
precision highp int;

const float _split = 8193.0;
const float pi = 3.141592654;
const float phi = 1.618033988;
const float _zero = 1e-45;
const float Sn = 1e-8;
const float Sp = 1e8;
const float Sh = 1e4;
const vec2 _0 = vec2(0.0);
const vec2 _1 = vec2(1.0, 0.0);
const vec2 _i = vec2(0.0, 1.0);
const vec2 _inf = vec2(1e38, 1e38);
const float specialValue = -1.23456788063;

const vec2 _pi = vec2(3.141593e+00, -8.742278e-08);
const vec2 _2pi = vec2(6.283185e+00, -1.748456e-07);
const vec2 _pi2 = vec2(1.570796e+00, -4.371139e-08);
const vec2 _pi4 = vec2(7.853982e-01, -2.185569e-08);
const vec2 _3pi4 = vec2(2.356194e+00, -5.962440e-09);
const vec2 _inv2pi = vec2(0.15915494, 6.4206382E-9);
const vec2 _e = vec2(2.718282e+00, 8.254840e-08);
const vec2 _log2 = vec2(6.931472e-01, -1.904654e-09);
const vec2 _log10 = vec2(2.302585e+00, -3.197544e-08);
const vec2 _pi16 = vec2(1.963495e-01, -5.463924e-09);
const float _eps = 1e-18;

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

const int n_fact = 9;
const float fact[9] = float[9](1.0, 1.0, 2.0, 6.0, 24.0, 120.0, 720.0, 5040.0, 40320.0);

const int n_gamma_vals = 8;
const float gamma_vals[8] = float[8](
    676.5203681218851,
    -1259.1392167224028,
    771.32342877765313,
    -176.61502916214059,
    12.507343278686905,
    -0.13857109526572012,
    9.9843695780195716e-6,
    1.5056327351493116e-7
);

const int n_primes = 25;
const float primes[25] = float[25](
    2.0,  3.0,  5.0,  7.0,  11.0,
    13.0, 17.0, 19.0, 23.0, 29.0,
    31.0, 37.0, 41.0, 43.0, 47.0,
    53.0, 59.0, 61.0, 67.0, 71.0,
    73.0, 79.0, 83.0, 89.0, 97.0
);


uniform uint maxIter;
uniform float R;
uniform float power;
uniform float x0;
uniform float y0;
uniform vec2 alpha0;
uniform vec2 j;
uniform vec2 p1;
uniform vec2 p2;
uniform vec2 p3;
uniform vec2 p4;
uniform vec2 q1;
uniform vec2 q2;
uniform vec2 q3;
uniform vec2 q4;
uniform vec2 xScale;
uniform vec2 yScale;
uniform vec2 xCoord;
uniform vec2 yCoord;
uniform float sinRotate;
uniform float cosRotate;
uniform highp sampler2D image;

in vec4 viewPos;
out uint fragmentColor;





uint packFloatsToUint(vec4 color) {
    uvec4 p = uvec4(color*254.0);
    return (p.r << 24) + (p.g << 16) + (p.b << 8) + p.a;
}

bool inRange(float a, float low, float high) {
    return a > low && a < high;
}
bvec2 inRange(vec2 a, float low, float high) {
    return bvec2(inRange(a.x, low, high), inRange(a.y, low, high));
}


vec2 complex(float a) { return vec2(a, 0.0); }
vec4 complex(vec2 a) { return vec4(a, _0); }

vec2 _float(vec2 z) { return z; }
vec2 _float(vec4 z) { return z.xz; }

vec2 _double(float a) { return vec2(a, 0.0); }
vec4 _double(vec2 z) { return vec4(z.x, 0.0, z.y, 0.0); }


vec2 quickTwoSum(float a, float b) {
    float s = a + b + _zero;
    float v = s - a;
    float e = b - v;
    return vec2(s, e);
}

vec2 twoSum(float a, float b) {
    float s = a + b + _zero;
    float v = s - a + _zero;
    float e = (a - (s - v)) + (b - v);
    return vec2(s, e);
}
vec4 twoSumComp(vec2 a, vec2 b) {
    vec2 s = a + b + _zero;
    vec2 v = s - a + _zero;
    vec2 e = (a - (s - v)) + (b - v);
    return vec4(s.x, e.x, s.y, e.y);
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

vec2 split1(float a) {
    mediump float a_hi = a;
    return vec2(a_hi, a - a_hi);
}

vec2 split2(float a) {
    float t = a*_split;
    float q = t - a + _zero;
    float a_hi = t - q;
    float a_lo = a - a_hi;
    return vec2(a_hi, a_lo);
}

// splitHandle

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






vec2 add(vec2 a, float b) {
    vec2 s;
    s = twoSum(a.x, b);
    s.y += a.y;
    s = quickTwoSum(s.x, s.y);
    s = quickTwoSum(s.x, s.y);
    return s;
}
vec2 add(float b, vec2 a) {
    return add(a, b);
}
vec2 add_old(vec2 a, vec2 b) {
    vec2 s = twoSum(a.x, b.x);
    vec2 t = twoSum(a.y, b.y);
    s.y += t.x;
    s = quickTwoSum(s.x, s.y);
    s.y += t.y;
    s = quickTwoSum(s.x, s.y);
    return s;
}
vec2 add(vec2 a, vec2 b) {
    vec4 st = twoSumComp(a, b);
    st.y += st.z;
    st.xy = quickTwoSum(st.x, st.y);
    st.y += st.w;
    st.xy = quickTwoSum(st.x, st.y);
    return st.xy;
}

vec2 sub(vec2 a, float b) { return add(a, -b); }
vec2 sub(float b, vec2 a) { return add(-a, b); }
vec2 sub(vec2 a, vec2 b) { return add(a, -b); }

vec2 mult(vec2 a, vec2 b) {
    vec2 p;
    p = twoProd(a.x, b.x);
    p.y += a.x*b.y + _zero;
    p.y += a.y*b.x + _zero;
    p = quickTwoSum(p.x, p.y);
    return p;
}
vec2 mult(float a, vec2 b) {
    vec2 p;
    p = twoProd(a, b.x);
    p.y += a*b.y;
    p = quickTwoSum(p.x, p.y);
    return p;
}
vec2 mult(vec2 a, float b) { return mult(b, a); }

vec2 sqr(vec2 a) {
    vec2 p = twoSqr(a.x);
    p.y += 2.0*a.x*a.y;
    p = quickTwoSum(p.x, p.y);
    return p;
}
vec2 cube(vec2 a) {
    return mult(sqr(a), a);
}
vec2 quad(vec2 a) {
    return sqr(sqr(a));
}
vec2 quint(vec2 a) {
    return mult(quad(a), a);
}
vec2 sext(vec2 a) {
    return sqr(cube(a));
}

vec2 sqrtd(vec2 a) {
    if (a == _0) return _0;
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
    float diff = add(a, -mult(b, yn)).x;
    vec2 prod = twoProd(xn, diff);
    return add(yn, prod);
}
vec2 div(vec2 a, float b) {
    float xn = 1.0/b;
    float yn = a.x*xn;
    float diff = add(a, -b*yn).x;
    vec2 prod = twoProd(xn, diff);
    return add(yn, prod);
}
vec2 div(float a, vec2 b) {
    float xn = 1.0/b.x;
    float yn = a*xn;
    float diff = add(a, -mult(b, yn)).x;
    vec2 prod = twoProd(xn, diff);
    return add(yn, prod);
}

vec2 inv(vec2 a) {
    float xn = 1.0/a.x;
    float diff = add(_1, -mult(a, vec2(xn, 0.0))).x;
    vec2 prod = twoProd(xn, diff);
    return add(vec2(xn, 0.0), prod);
}

vec2 absd(vec2 a) {
    if (a.x < 0.0) { return -a; }
    else { return a; }
}

vec2 modd(vec2 a, float b) {
    int n = int(div(a, vec2(b, 0.0)).x);  // optimize ??
    return add(a, -mult(b, vec2(float(n), 0.0)));
}

bool gtd(vec2 a, float b) {
    if (a.x > b || (a.x == b && a.y > 0.0)) return true;
    else return false;
}
bool ltd(vec2 a, float b) {
    if (a.x < b || (a.x == b && a.y < 0.0)) return true;
    else return false;
}

vec2 clampd(vec2 a, float min, float max) {
    vec2 b = a;
    if (gtd(a, max)) b = vec2(max, 0.0);
    else if (ltd(a, min)) b = vec2(min, 0.0);
    return b;
}

vec2 sind_taylor(vec2 a) {

    float thresh = 0.5*abs(a.x)*_eps;
    vec2 r, s, t, x;

    if (a == _0) { return _0; }

    x = -sqr(a);
    s = a;
    r = a;

    for (int i = 0; i < n_inv_fact; i += 2) {
        r = mult(r, x);
        t = mult(r, inv_fact[i]);
        s = add(s, t);
        if (abs(t.x) < thresh) break;
    }

    return s;
}

vec2 cosd_taylor(vec2 a) {

    float thresh = 0.5*_eps;
    vec2 r, s, t, x;

    if (a == _0) { return _1; }

    x = -sqr(a);
    r = x;
    s = add(_1, 0.5*r);

    for (int i = 1; i < n_inv_fact; i += 2) {
        r = mult(r, x);
        t = mult(r, inv_fact[i]);
        s = add(s, t);
        if (abs(t.x) < thresh) break;
    }

    return s;

}

void sincosd_taylor(vec2 a, out vec2 sin_a, out vec2 cos_a) {

    if (a == _0) {
        sin_a = _0;
        cos_a = _1;
        return;
    }

    sin_a = sind_taylor(a);
    cos_a = sqrtd(add(_1, -sqr(sin_a)));
    // cos_a = cosd_taylor(a);

}

float signd(vec2 a) {
    if (a.x == 0.0) return sign(a.y);
    else return sign(a.x);
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

    if (a == _0) { return _0; }

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
    if (j < -2 || j > 2) { return _0; }
    if (abs_k > 4) { return _0; }


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

        if (k > 0)  r = add(mult(u, sin_t), mult(v, cos_t));
        else        r = add(mult(u, sin_t), -mult(v, cos_t));

    }
    else if (j == 1) {

        if (k > 0)  r = add(mult(u, cos_t), -mult(v, sin_t));
        else        r = add(mult(u, cos_t), mult(v, sin_t));

    }
    else if (j == -1) {

        if (k > 0)  r = add(mult(v, sin_t), -mult(u, cos_t));
        else        r = add(mult(-u, cos_t), -mult(v, sin_t));

    }
    else {

        if (k > 0)  r = add(mult(-u, sin_t), -mult(v, cos_t));
        else        r = add(mult(v, cos_t), -mult(u, sin_t));

    }

    return r;

}

vec2 cosd(vec2 a) {

    if (a == _0) { return _1; }
    
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
    
    if (j < -2 || j > 2) { return _0; }
    if (abs_k > 4) { return _0; }
    
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

    if (a == _0) {
        sin_a = _0;
        cos_a = _1;
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
        cos_a = _0;
        sin_a = _0;
        return;
    }
    
    if (abs_k > 4) {
        cos_a = _0;
        sin_a = _0;
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

vec2 tand(vec2 a) {
    if (modd(sub(a, _pi2), pi) == _0) return _inf;
    vec2 sina, cosa;
    sincosd(a, sina, cosa);
    return div(sina, cosa);
}

float cscd(float a) {
    if (mod(a, pi) == 0.0) return _inf.x;
    return 1.0/sin(a);
}
vec2 cscd(vec2 a) {
    if (modd(a, pi) == _0) return _inf;
    return inv(sind(a));
}

float secd(float a) {
    if (mod(a - 0.5*pi, pi) == 0.0) return _inf.x;
    return 1.0/cos(a);
}
vec2 secd(vec2 a) {
    if (modd(sub(a, _pi2), pi) == _0) return _inf;
    return inv(cosd(a));
}

float cotd(float a) {
    if (mod(a, pi) == 0.0) return _inf.x;
    return cos(a)/sin(a);
}
vec2 cotd(vec2 a) {
    if (modd(a, pi) == _0) return _inf;
    vec2 sina, cosa;
    sincosd(a, sina, cosa);
    return div(cosa, sina);
}

vec2 atan2d(vec2 y, vec2 x) {

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

    if (x == _0) {
        if (y == _0) { return _0; }
        if (gtd(y, 0.0)) return _pi2;
        else return -_pi2;
    }
    else if (y == _0) {
        if (gtd(x, 0.0)) return _0;
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

vec2 asind(vec2 a) {
    vec2 abs_a = absd(a);

    if (gtd(abs_a, 1.0)) return _0; // out of domain

    if (abs_a == _1) {
        if (a.x == 1.0) return _pi2;
        else            return -_pi2;
    }

    return atan2d(a, sqrtd(sub(1.0, sqr(a))));
}

vec2 acosd(vec2 a) {
    vec2 abs_a = absd(a);

    if (gtd(abs_a, 1.0)) return _0; // out of domain

    if (abs_a == _1) {
        if (a.x == 1.0) return _0;
        else            return _pi;
    }

    return atan2d(sqrtd(sub(1.0, sqr(a))), a);
}

vec2 atand(vec2 a) {
    return atan2d(a, _1);
}

float acsc(float a) {
    if (abs(a) < 1.0) return 0.0; // out of domain
    return asin(1.0/a);
}
vec2 acscd(vec2 a) {
    if (ltd(absd(a), 1.0)) return _0; // out of domain
    return asind(inv(a));
}

float asec(float a) {
    if (abs(a) < 1.0) return 0.0; // out of domain
    return acos(1.0/a);
}
vec2 asecd(vec2 a) {
    if (ltd(absd(a), 1.0)) return _0; // out of domain
    return acosd(inv(a));
}

float acot(float a) {
    if (a == 0.0) return _pi2.x;
    return atan(1.0/a);
}
vec2 acotd(vec2 a) {
    if (a == _0) return _pi2;
    return atand(inv(a));
}

vec2 expd(vec2 a) {

    /* Strategy:  We first reduce the size of x by noting that
       
            exp(kr + m * log(2)) = 2^m * exp(r)^k
    
       where m and k are integers.  By choosing m appropriately
       we can make |kr| <= log(2) / 2 = 0.347.  Then exp(r) is 
       evaluated using the familiar Taylor series.  Reducing the 
       argument substantially speeds up the convergence.       */

//    float k = 512.0;
    float k = 16.0;
    float inv_k = 1.0 / k;
    
    if (a.x <= -709.0) return _0;
    if (a.x >=  709.0) return _0; // inf
    if (a == _0) return _1;
    if (a == _1) return _e;
    
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
//    s = add(2.0*s, sqr(s));
//    s = add(2.0*s, sqr(s));
//    s = add(2.0*s, sqr(s));
//    s = add(2.0*s, sqr(s));
//    s = add(2.0*s, sqr(s));
    s = add(s, _1);
    
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

    if (a == _1) { return _0; }
    if (a.x <= 0.0) { return _0; } // error

    vec2 x = vec2(log(a.x), 0.0);

    x = add(add(x, mult(a, expd(-x))), -_1);
    return x;

}

vec2 sinhd(vec2 a) {

//    if (a == ZERO) return ZERO;
//    vec2 ea = expd(a);
//    return 0.5*(ea - inv(ea));

    float thresh = 0.5*abs(a.x)*_eps;
    vec2 r, s, t, x;

    if (a == _0) { return _0; }

    x = sqr(a);
    s = a;
    r = a;

//    r = mult(r, x);
//    t = mult(r, inv_fact[0]);
//    s = add(s, t);
//    if (abs(t.x) > thresh) return s;

    for (int i = 0; i < n_inv_fact; i += 2) {
        r = mult(r, x);
        t = mult(r, inv_fact[i]);
        s = add(s, t);
        if (abs(t.x) < thresh) break;
    }

    return s;

}

vec2 coshd(vec2 a) {

//    if (a == ZERO) return ONE;
//    vec2 ea = expd(a);
//    return 0.5*(ea + inv(ea));

    float thresh = 0.5*_eps;
    vec2 r, s, t, x;

    if (a == _0) { return _1; }

    x = sqr(a);
    r = x;
    s = add(0.5*r, 1.0);

//    r = mult(r, x);
//    t = mult(r, inv_fact[1]);
//    s = add(s, t);
//    if (abs(t.x) > thresh) return s;

    for (int i = 1; i < n_inv_fact; i += 2) {
        r = mult(r, x);
        t = mult(r, inv_fact[i]);
        s = add(s, t);
        if (abs(t.x) < thresh) break;
    }

    return s;

}

void sinhcoshd(vec2 a, out vec2 sinh_a, out vec2 cosh_a) {

    if (a == _0) {
        sinh_a = _0;
        cosh_a = _1;
        return;
    }

//    vec2 t1 = expd(a);
//    vec2 t2 = inv(t1);
//    sinh_a = 0.5*add(t1, -t2);
//    cosh_a = 0.5*add(t1, t2);

//    sinh_a = sinhd(a);
    cosh_a = coshd(a);
    sinh_a = signd(a)*sqrtd(add(sqr(cosh_a), -1.0));
//    cosh_a = sqrtd(add(sqr(sinh_a), 1.0));

}

vec2 tanhd(vec2 a) {

    vec2 sinha, cosha;
    sinhcoshd(a, sinha, cosha);
    return div(sinha, cosha);

}

float cschd(float a) {
    if (a == 0.0) return _inf.x;
    return 1.0/sinh(a);
}
vec2 cschd(vec2 a) {
    if (a == _0) return _inf;
    return inv(sinhd(a));
}

float sechd(float a) {
    return 1.0/cosh(a);
}
vec2 sechd(vec2 a) {
    return inv(coshd(a));
}

float cothd(float a) {
    if (a == 0.0) return _inf.x;
    return cosh(a)/sinh(a);
}
vec2 cothd(vec2 a) {
    if (a == _0) return _inf;
    vec2 sinha, cosha;
    sinhcoshd(a, sinha, cosha);
    return div(cosha, sinha);
}

vec2 asinhd(vec2 a) {
    return logd(add(a, sqrtd(add(sqr(a), 1.0))));
}

vec2 acoshd(vec2 a) {
    if (ltd(a, 1.0)) return _0; // out of domain
    return logd(add(a, sqrtd(sub(sqr(a), 1.0))));
}

vec2 atanhd(vec2 a) {
    vec2 abs_a = absd(a);
    if (gtd(abs_a, 1.0) || abs_a == _1) return _0; // out of domain
    return 0.5*(logd(div(add(1.0, a), sub(1.0, a))));
}

float acsch(float a) {
    if (a == 0.0) return _inf.x;
    float ainv = 1.0/a;
    return log(ainv + sqrt(ainv*ainv + 1.0));
}
vec2 acschd(vec2 a) {
    if (a == _0) return _inf;
    vec2 ainv = inv(a);
    return logd(add(ainv, sqrtd(add(sqr(ainv), 1.0))));
}

float asech(float a) {
    if (a == 0.0) return _inf.x;
    else if (a < 0.0 || a > 1.0) return 0.0; // out of domain
    return log((1.0 + sqrt(1.0 - a*a))/a);
}
vec2 asech(vec2 a) {
    if (a == _0) return _inf;
    else if (ltd(a, 0.0) || gtd(a, 1.0)) return _0; // out of domain
    return logd(div(add(1.0, sqrtd(sub(1.0, sqr(a)))), a));
}

float acoth(float a) {
    if (a == 1.0) return _inf.x;
    else if (a == -1.0) return -_inf.x;
    else if (abs(a) < 1.0) return 0.0; // out of domain
    return 0.5*log((a + 1.0)/(a - 1.0));
}
vec2 acoth(vec2 a) {
    if (a == _1) return _inf;
    else if (a == -_1) return -_inf;
    else if (ltd(absd(a), 1.0)) return _0; // out of domain
    return 0.5*logd(div(add(a, 1.0), sub(a, 1.0)));
}

vec2 powd(vec2 a, float b) { return expd(mult(b, logd(a))); }
vec2 powd(float a, vec2 b) { return expd(mult(b, log(a))); }
vec2 powd(vec2 a, vec2 b) { return expd(mult(b, logd(a))); }





bool isSpecial(float a) { return isinf(a) || isnan(a); }

float excludeSpecial(float a) {
    float b = a;
    if (isinf(a) || isnan(a)) b = _zero*texture(image, _0).x;
    return b;
}

vec2 cadd(vec2 z, float a) {
    return vec2(z.x + a, z.y);
}
vec2 cadd(vec2 z, vec2 w) {
    return z + w;
}
vec2 cadd(float a, vec2 z) { return cadd(z, a); }
vec4 cadd(vec4 z, float a) {
    return vec4(add(z.xy, complex(a)), z.zw);
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

float arg(float a) {
    if (a > 0.0) return 0.0;
    else return pi;
}
float arg(vec2 a) {
    if (gtd(a, 0.0)) return 0.0;
    else return pi;
}
float carg(vec2 z) {
    return atan(z.y, z.x);
}
vec2 carg(vec4 z) {
    return atan2d(z.zw, z.xy);
}

vec2 conj(vec2 z) {
    return vec2(z.x, -z.y);
}
vec4 conj(vec4 z) { return vec4(z.xy, -z.zw); }

vec2 cmult(vec2 z, vec2 w) {
    float k1 = w.x*(z.x + z.y);
    float k2 = z.x*(w.y - w.x);
    float k3 = z.y*(w.x + w.y);
    return vec2(k1 - k3, k1 + k2);
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
    // vec2 k1 = mult(u, add(x, y));
    // vec2 k2 = mult(x, sub(v, u));
    // vec2 k3 = mult(y, add(u, v));
    // return vec4(sub(k1, k3), add(k1, k2));
    return vec4(
        add(mult(x, u), -mult(y, v)),
        add(mult(x, v), mult(y, u))
    );
}
vec4 cmult(vec2 z, vec4 w) { return cmult(_double(z), w); }
vec4 cmult(vec4 w, vec2 z) { return cmult(z, w); }

vec2 cmulti(vec2 z) { return vec2(-z.y, z.x); }
vec4 cmulti(vec4 z) { return vec4(-z.zw, z.xy); }

float sqr(float a) {
    return a*a;
}
vec2 csqr(vec2 z) {
    float k1 = z.x*(z.x + z.y);
    float k2 = z.x*(z.y - z.x);
    float k3 = z.y*(z.x + z.y);
    return vec2(k1 - k3, k1 + k2);
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

float sext(float a) { return a*quint(a); }
vec2 csext(vec2 z) {
    float xsqr = z.x*z.x;
    float ysqr = z.y*z.y;
    float a1 = 3.0*xsqr;
    float a2 = 2.0*a1;
    float a3 = a2 + 4.0*xsqr;
    float b1 = 3.0*ysqr;
    float b2 = 2.0*b1;
    float b3 = b2 + 4.0*ysqr;
    return vec2(
        xsqr*xsqr*(xsqr - b1) - 12.0*xsqr*ysqr*(xsqr - ysqr) + ysqr*ysqr*(a1 - ysqr),
        z.x*xsqr*z.y*(a2 - b3) - z.x*z.y*ysqr*(a3 - b2)
    );
}
vec4 csext(vec4 z) {
    vec2 xsqr = sqr(z.xy);
    vec2 ysqr = sqr(z.zw);
    vec2 a1 = mult(3.0, xsqr);
    vec2 a2 = 2.0*a1;
    vec2 a3 = add(a2, 4.0*xsqr);
    vec2 b1 = mult(3.0, ysqr);
    vec2 b2 = 2.0*b1;
    vec2 b3 = add(b2, 4.0*ysqr);
    return vec4(
        add(sub(mult(xsqr, mult(xsqr, sub(xsqr, b1))), mult(12.0, mult(xsqr, mult(ysqr, sub(xsqr, ysqr))))), mult(ysqr, mult(ysqr, sub(a1, ysqr)))),
        sub(mult(z.xy, mult(xsqr, mult(z.zw, sub(a2, b3)))), mult(z.xy, mult(z.zw, mult(ysqr, sub(a3, b2)))))
    );
}

float cmodsqr(vec2 z) {
    return dot(z, z);
}
vec2 cmodsqr(vec4 z) {
    return add(sqr(z.xy), sqr(z.zw));
}

float cmod(vec2 z) {
    return length(z);
}
vec2 cmod(vec4 z) {
    return sqrtd(cmodsqr(z));
}
float cmod2(vec2 z) {
    float a = z.y/z.x;
    return abs(z.x)*sqrt(1.0 + a*a);
}
vec2 cmod2(vec4 z) {
    vec2 a = div(z.zw, z.xy);
    return mult(absd(z.xy), sqrtd(_1 + sqr(a)));
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

vec2 cdiv(float a, vec2 w) {
    if (w == _0) return _inf;
    return a*conj(w)/dot(w, w);
}
vec2 cdiv(vec2 z, vec2 w) {
    if (w == _0) return _inf;
    vec2 u = cmult(z, conj(w));
    return u/dot(w, w);
}
vec4 cdiv(float a, vec4 w) {
    if (w == vec4(0.0)) return vec4(_inf, _inf);
    vec4 u = cmult(a, conj(w));
    vec2 s = cmodsqr(w);
    return vec4(div(u.xy, s), div(u.zw, s));
}
vec4 cdiv(vec4 z, float a) {
    if (a == 0.0) return vec4(_inf, _inf);
    vec2 b = vec2(a, 0.0);
    return vec4(div(z.xy, b), div(z.zw, b));
}
vec4 cdiv(vec4 z, vec4 w) {
    if (w == vec4(0.0)) return vec4(_inf, _inf);
    vec2 s1 = cmodsqr(w);
    vec4 s2 = cmult(z, conj(w));
    vec2 p = div(s2.xy, s1);
    vec2 q = div(s2.zw, s1);
    return vec4(p, q);
}
vec4 cdiv(vec4 z, vec2 w) { return cdiv(z, _double(w)); }
vec4 cdiv(vec2 z, vec4 w) { return cdiv(_double(z), w); }

vec2 csign(vec2 z) {
    if (z == _0) return vec2(_inf);
    return z/cmod(z);
}
vec4 csign(vec4 z) {
    if (z == vec4(0.0)) return vec4(_inf, _inf);
    return cdiv(z, complex(cmod(z)));
}

vec2 csqrt(vec2 z) {
    if (z == _0) return _0;
    float modz = cmod(z);
    float a = modz + z.x;
    float b = modz - z.x;
    return vec2(
        sqrt(0.5*a),
        sign(z.y)*sqrt(0.5*b)
        //sqrt(modz)*sin(0.5*carg(z))
    );
}
vec4 csqrt(vec4 z) {
    if (z == vec4(0.0)) return vec4(0.0);
    vec2 modz = cmod(z);
    vec2 a = add(modz, z.xy);
    vec2 b = sub(modz, z.xy);
    return vec4(
        sqrtd(0.5*a),
        signd(z.zw)*sqrtd(0.5*b)
    );
}

vec2 cexp(vec2 w) {
    if (w.y == 0.0) return complex(exp(w.x));
    else return exp(w.x)*vec2(cos(w.y), sin(w.y));
}
vec4 cexp(vec4 w) {
    vec2 t = expd(w.xy);
    if (w.zw == _0) return vec4(t, _0);
    else {
        vec2 siny, cosy;
        sincosd(w.zw, siny, cosy);
        return vec4(mult(t, cosy), mult(t, siny));
    }
}

vec2 cexp2(vec2 w) {
    return cexp(_log2.x*w);
}
vec4 cexp2(vec4 w) {
    return cexp(cmult(complex(_log2), w));
}

vec2 cexp10(vec2 w) {
    return cexp(_log10.x*w);
}
vec4 cexp10(vec4 w) {
    return cexp(cmult(complex(_log10), w));
}

vec2 clog(vec2 z) {
    if (z == _0) return vec2(-2e28, 0.0);
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
    if (z == vec4(0.0)) return vec4(-2e28, 0.0, 0.0, 0.0);
    vec2 rsqr = cmodsqr(z);
    vec2 theta;
    if (z.zw == _0) {
        if (gtd(z.xy, 0.0)) theta = _0;
        else theta = _pi;
    }
    else theta = carg(z);
    return vec4(
        0.5*logd(rsqr),
        theta
    );
}

vec2 clog2(vec2 z) {
    return clog(z)/_log2.x;
}
vec4 clog2(vec4 z) {
    return cdiv(clog(z), complex(_log2));
}

vec2 clog10(vec2 z) {
    return clog(z)/_log10.x;
}
vec4 clog10(vec4 z) {
    return cdiv(clog(z), complex(_log10));
}

vec2 clog(vec2 z, float b) {
    return clog(z)/log(b);
}
vec4 clog(vec4 z, float b) {
    return cdiv(clog(z), complex(log(b)));
}

vec2 cpow(float x, vec2 s) {

    float theta;
    float c;
    float f;
    if (x == 0.0) return _0;
    else if (s == _0) return _1;
    else {
        float lnx = log(abs(x));
        if (x > 0.0) {
             c = exp(s.x*lnx);
//            c = pow(x, s.x);
            f = s.y*lnx;
        }
        else {
             c = exp(s.x*lnx - s.y*pi);
//            c = pow(x, s.x)*exp(-s.y*pi);
            f = s.x*pi + s.y*lnx;
        }
    }



    return c*vec2(cos(f), sin(f));

}
vec4 cpow(vec2 x, vec4 s) {

    vec2 theta;
    if (x == _0) return vec4(_0, _0);
    else if (gtd(x, 0.0)) theta = _0;
    else theta = _pi;

    vec2 lnx = logd(absd(x));

    vec2 c, f, sinf, cosf;
    if (s.zw == _0) {
        if (theta == _0) return vec4(powd(x, s.xy), _0);
        c = expd(mult(s.xy, lnx));
        f = mult(s.xy, theta);
    }
    else if (s.xy == _0) {
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
vec4 cpow(float x, vec4 s) {

    vec2 theta;
    if (x == 0.0) return vec4(0.0);
    else if (x > 0.0) theta = _0;
    else theta = _pi;

    float lnx = log(abs(x));

    vec2 c, f, sinf, cosf;
    if (s.zw == _0) {
        if (theta == _0) return vec4(powd(x, s.xy), _0);
        c = expd(mult(s.xy, lnx));
        f = mult(s.xy, theta);
    }
    else if (s.xy == _0) {
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

vec2 cpow(vec2 z, int n) {
    vec2 w = z;
    for (int i = 1; i < n; i++) w = cmult(w, z);
    return w;
}
vec4 cpow(vec4 z, int n) {
    vec4 w = z;
    for (int i = 1; i < n; i++) w = cmult(w, z);
    return w;
}

vec2 cpow(vec2 z, float p) {
    if (p == 1.0) return z;
    else if (p == 2.0) return csqr(z);
    else if (p == 3.0) return ccube(z);
    else if (p == 4.0) return cquad(z);
    else if (p == 5.0) return cquint(z);
    else if (p == -1.0) return cinv(z);
    else if (p == -2.0) return cinv(csqr(z));
    else if (p == -3.0) return cinv(ccube(z));
    else if (p == -4.0) return cinv(cquad(z));
    else if (p == -5.0) return cinv(cquint(z));
    else return cexp(p*clog(z));
}
vec4 cpow(vec4 z, float p) {
    return cexp(cmult(p, clog(z)));
}

vec2 cpow(vec2 z, vec2 s) {
    if (s.y == 0.0) return cpow(z, s.x);
    return cexp(cmult(s, clog(z)));
}
vec4 cpow(vec4 z, vec4 s) {
    if (s.zw == _0) {
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
    vec2 _w = _1*Sn;
    vec2 _zPow = _1*Sn;
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

vec2 sin_taylor(vec2 z) {
    vec2 w = ccube(z);
    vec2 t2 = w*inv_fact[0].x;
    w = cmult(w, csqr(w));
    vec2 t3 = w*inv_fact[2].x;
    return z - t2 + t3;
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

    vec2 sinx, cosx;
    sincosd(z.xy, sinx, cosx);

    vec2 sinhy, coshy;
    sinhcoshd(z.zw, sinhy, coshy);

    return vec4(
        mult(sinx, coshy),
        mult(cosx, sinhy)
    );

}

vec2 ccos(vec2 z) {
    return vec2(
        cos(z.x)*cosh(z.y),
        -sin(z.x)*sinh(z.y)
    );
}
vec4 ccos(vec4 z) {
    vec2 sinx, cosx;
    sincosd(z.xy, sinx, cosx);
    vec2 sinhy, coshy;
    sinhcoshd(z.zw, sinhy, coshy);
    return vec4(
        mult(cosx, coshy),
        -mult(sinx, sinhy)
    );
}

vec2 ctan(vec2 z) {
    vec2 cosz = ccos(z);
    if (cosz == _0) return _inf;
    return cdiv(csin(z), cosz);
}
vec4 ctan(vec4 z) {
    vec4 cosz = ccos(z);
    if (cosz == vec4(0.0)) return vec4(_inf, _inf);
    return cdiv(csin(z), cosz);
}

vec2 ccsc(vec2 z) {
    vec2 sinz = csin(z);
    if (sinz == _0) return _inf;
    return cinv(sinz);
}
vec4 ccsc(vec4 z) {
    vec4 sinz = csin(z);
    if (sinz == vec4(0.0)) return vec4(_inf, _inf);
    return cinv(sinz);
}

vec2 csec(vec2 z) {
    vec2 cosz = ccos(z);
    if (cosz == _0) return _inf;
    return cinv(cosz);
}
vec4 csec(vec4 z) {
    vec4 cosz = ccos(z);
    if (cosz == vec4(0.0)) return vec4(_inf, _inf);
    return cinv(ccos(z));
}

vec2 ccot(vec2 z) {
    vec2 sinz = csin(z);
    if (sinz == _0) return _inf;
    return cdiv(sinz, ccos(z));
}
vec4 ccot(vec4 z) {
    vec4 sinz = csin(z);
    if (sinz == vec4(0.0)) return vec4(_inf, _inf);
    return cdiv(sinz, ccos(z));
}

vec2 casin(vec2 z) {
    return cmult(_i, clog(csqrt(_1 - csqr(z)) - cmult(_i, z)));
}
vec4 casin(vec4 z) {
    return cmult(_i, clog(csqrt(cadd(_1, -csqr(z))) - cmult(_i, z)));
}

vec2 cacos(vec2 z) {
    return vec2(0.5*pi, 0.0) - casin(z);
}
vec4 cacos(vec4 z) {
    return cadd(complex(_pi2), -casin(z));
}

vec2 catan(vec2 z) {
    vec2 iz = cmult(_i, z);
    return -0.5*cmult(_i, clog(cdiv(_1 + iz, _1 - iz)));
}
vec4 catan(vec4 z) {
    vec4 iz = cmult(_i, z);
    return -0.5*cmult(_i, clog(cdiv(cadd(1.0, iz), cadd(1.0, -iz))));
}

vec2 cacsc(vec2 z) {
    return cmult(_i, clog(csqrt(_1 - cinv(csqr(z))) - cdiv(_i, z)));
}
vec4 cacsc(vec4 z) {
    return cmult(_i, clog(cadd(csqrt(cadd(1.0, -cinv(csqr(z)))), -cdiv(_i, z))));
}

vec2 casec(vec2 z) {
    return vec2(0.5*pi, 0.0) - cacsc(z);
}
vec4 casec(vec4 z) {
    return cadd(complex(_pi2), -cacsc(z));
}

vec2 cacot(vec2 z) {
    vec2 iz = cmult(_i, z);
    return -0.5*cmult(_i, clog(cdiv(iz - _1, iz + _1)));
}
vec4 cacot(vec4 z) {
    vec4 iz = cmult(_i, z);
    return -0.5*cmult(_i, clog(cdiv(cadd(iz, -1.0), cadd(iz, 1.0))));
}

vec2 csinh(vec2 z) {
    return vec2(
        cos(z.y)*sinh(z.x),
        sin(z.y)*cosh(z.x)
    );
}
vec4 csinh(vec4 z) {
    vec2 siny, cosy;
    sincosd(z.zw, siny, cosy);
    vec2 sinhx, coshx;
    sinhcoshd(z.xy, sinhx, coshx);
    return vec4(
        mult(cosy, sinhx),
        mult(siny, coshx)
    );
}

vec2 ccosh(vec2 z) {
    return vec2(
        cos(z.y)*cosh(z.x),
        sin(z.y)*sinh(z.x)
    );
}
vec4 ccosh(vec4 z) {
    vec2 siny, cosy;
    sincosd(z.zw, siny, cosy);
    vec2 sinhx, coshx;
    sinhcoshd(z.xy, sinhx, coshx);
    return vec4(
        mult(cosy, coshx),
        mult(siny, sinhx)
    );
}

vec2 ctanh(vec2 z) {
    if (z.x == 0.0 && fract(z.y/pi + 0.5) == 0.0) return _inf;
    return cdiv(csinh(z), ccosh(z));
}
vec4 ctanh(vec4 z) {
    if (z.xy == _0 && fract(z.z/pi + 0.5) == 0.0) return vec4(_inf, _inf);
    return cdiv(csinh(z), ccosh(z));
}

vec2 ccsch(vec2 z) {
    if (z == _0) return _inf;
    else return cinv(csinh(z));
}
vec4 ccsch(vec4 z) {
    if (z == vec4(0.0)) return _inf.xxxx;
    else return cinv(csinh(z));
}

vec2 csech(vec2 z) { return cinv(ccosh(z)); }
vec4 csech(vec4 z) { return cinv(ccosh(z)); }

vec2 ccoth(vec2 z) {
    if (z == _0) return _inf;
    else return cinv(ctanh(z));
}
vec4 ccoth(vec4 z) {
    if (z == vec4(0.0)) return _inf.xxxx;
    else return cinv(ctanh(z));
}

vec2 casinh(vec2 z) {
    return clog(z + csqrt(csqr(z) + _1));
}
vec4 casinh(vec4 z) {
    return clog(cadd(z, csqrt(cadd(csqr(z), 1.0))));
}

vec2 cacosh(vec2 z) {
    return clog(z + cmult(csqrt(z + _1), csqrt(z - _1)));
}
vec4 cacosh(vec4 z) {
    return clog(cadd(z, cmult(csqrt(cadd(z, 1.0)), csqrt(cadd(z, -1.0)))));
}

vec2 catanh(vec2 z) {
    return 0.5*clog(cdiv(_1 + z, _1 - z));
}
vec4 catanh(vec4 z) {
    return 0.5*clog(cdiv(cadd(1.0, z), cadd(1.0, -z)));
}

vec2 cacsch(vec2 z) {
    return clog(cinv(z) + csqrt(cinv(csqr(z)) + _1));
}
vec4 cacsch(vec4 z) {
    return clog(cadd(cinv(z), csqrt(cadd(cinv(csqr(z)), 1.0))));
}

vec2 casech(vec2 z) {
    vec2 zinv = cinv(z);
    return clog(zinv + cmult(csqrt(zinv + _1), csqrt(zinv - _1)));
}
vec4 casech(vec4 z) {
    vec4 zinv = cinv(z);
    return clog(cadd(zinv, cmult(csqrt(cadd(zinv, 1.0)), csqrt(cadd(zinv, -1.0)))));
}

vec2 cacoth(vec2 z) {
    return 0.5*clog(cdiv(z + _1, z - _1));
}
vec4 cacoth(vec4 z) {
    return 0.5*clog(cdiv(cadd(z, 1.0), cadd(z, -1.0)));
}

vec2 bessel(vec2 z) {

    if (z == _0) return _1;
    vec2 t1 = ccos(0.5*z);
    vec2 t2 = 2.0*csqr(t1) - _1;
    float modz = cmod(z);

    vec2 low = vec2(1.0/6.0, 0.0) + t1/3.0 + ccos(0.5*sqrt(3.0)*z)/3.0 + t2/6.0;
//    vec2 high = cmult(csqrt(2.0*cinv(pi*z)), ccos(z - vec2(_pi4.x, 0.0)) + vec2(exp(abs(z.y))/cmod(z), 0.0));
    vec2 high2 = sqrt(2.0/(pi*modz))*ccos((1.0 - 0.25*pi/modz)*z);
    float q = 1.0/(exp(20.0*log(abs(z.x)/7.0)) + 1.0);
//    return cmult(1.0/6.0 + ccos(0.5*z)/3.0 + ccos(0.5*sqrt(3.0)*z)/3.0 + ccos(z)/6.0, q) + sqrt(2.0/(pi*cmod(z)))*cmult(ccos(z - 0.25*pi*z/cmod(z)), ONE - q);
//    return cmult(low, q) + cmult(high2, ONE - q);
    return q*low + (1.0 - q)*high2;

}
vec4 bessel(vec4 z) {

    if (z == vec4(0.0)) return vec4(1.0, 0.0, 0.0, 0.0);
    vec4 t1 = ccos(0.5*z);
    vec4 t2 = cadd(2.0*csqr(t1), -1.0);
    vec2 modz = cmod(z);

    vec4 low = cadd(1.0/6.0, cadd(cdiv(t1, 3.0), cadd(cdiv(ccos(0.5*cmult(sqrt(3.0), z)), 3.0), cdiv(t2, 6.0))));
    vec4 high = cmult(sqrtd(div(_double(2.0/pi), modz)), ccos(cmult(add(1.0, div(-_pi4, modz)), z)));
    float q = 1.0/(exp(20.0*log(abs(z.x)/7.0)) + 1.0);
    return cadd(cmult(q, low), cmult(1.0 - q, high));

}

vec2 gamma(vec2 z) {

    // infinite product method
//    if (z.y == 0.0 && z.x <= 0.0 && fract(z.x) == 0.0) { return INF; }
//    float thresh = _eps;
//    vec2 p = ONE;
//    vec2 prev;
//    int i = 1;
//    do {
//        prev = p;
//        p = cmult(p, cdiv(cpow(1.0 + 1.0/float(i), z), ONE + z/float(i)));
//        i++;
//    } while (i < 35);
//    return cdiv(p, z);


    // Stirling approximation
//    return cmult(cmult(csqrt(cdiv(2.0*pi, z)), cpow(z/_e.x, z)), ONE + cinv(z));


    // Lanczos approximation
    vec2 w = z;
    if (z.x < 0.5) w = _1 - z;
    w.x -= 1.0;
    vec2 x = vec2(0.99999999999980993, 0.0);
    for (int i = 0; i < n_gamma_vals; i++) {
        x += cdiv(gamma_vals[i], (w + vec2(float(i) + 1.0, 0.0)));
    }
    vec2 t = w + vec2(float(n_gamma_vals) - 0.5, 0.0);
    vec2 y = sqrt(2.0*pi)*cmult(cpow(t, w + vec2(0.5, 0.0)), cmult(cexp(-t), x));

//    if (abs(y.y) < 1e-7) y.y = 0.0;
    if (z.x < 0.5) y = cdiv(pi, cmult(csin(pi*z), y));

    return y;


    // Nemes approximation
//    return cexp(0.5*(vec2(log(2.0*pi), 0.0) - clog(z)) + cmult(z, clog(z + cinv(12.0*z - cinv(10.0*z))) - ONE));
//    return cmult(csqrt(2.0*pi*cinv(z)), cpow((z + cinv(12.0*z - cinv(10.0*z)))/_e.x, z));

}

vec2 zeta(vec2 z) {

    if (z.y == 0.0) {
        if (z.x == 0.0) return vec2( -0.5,  0.0  );
        if (z.x == 1.0) return vec2(  1e38, 1e38 );
    }

    vec2 w = z;
    if (z.x <= 1.0) w = _1 - z;

    vec2 s1 = _0;
    vec2 s2;
    float sgn;
    int N = n_fact;

    for (int n = 0; n < N; n++) {
        s2 = _0;
        sgn = 1.0;
        for (int k = 0; k <= n; k++) {
            s2 += sgn*(fact[n]/(fact[k]*fact[n - k]))*cpow(float(k + 1), -w);
            sgn *= -1.0;
        }
        s1 += s2 / exp2(float(n + 1));
    }
    s1 = cdiv(s1, _1 - cexp2(_1 - w));

    // infinite product of inverse primes
//    vec2 p = ONE;
//    int n = 0;
//    while (n < n_primes) {
//        p = cmult(p, cinv(ONE - cpow(primes[n], -w)));
//        n++;
//    }

    // infinite sum of inverse integers
//    vec2 s = ZERO;
//    int n = 0;
//    while (n < 100) {
//        s += cinv(cpow(float(n + 1), w));
//        n++;
//    }

    // reflection formula
    if (z.x <= 1.0) {
        s1 = cdiv(cmult(cmult(gamma(0.5*w), cpow(pi, -0.5*w)), s1), cmult(gamma(0.5*z), cpow(pi, -0.5*z)));
        // s1 = cdiv(cmult(cpow(_2pi.x, w), s1), 2.0*cmult(ccos(0.5*pi*w), gamma(w)));
    }
    return s1;

}

vec2 eta(vec2 z) {
    return cmult(_1 - cexp2(_1 - z), zeta(z));
}

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
    if (x.x < -1.0) { u = add(-2.0*_1, -x); }
    else if (x.x > 1.0) { u = add(2.0*_1, -x); }
    if (y.x < -1.0) { v = add(-2.0*_1, -y); }
    else if (y.x > 1.0) { v = add(2.0*_1, -y); }
    return vec4(u, v);
}

vec2 ballfold(vec2 z) {
    if (z == _0) return z;
    else return clamp(1.0/cmodsqr(z), 1.0, 4.0)*z;
}
vec4 ballfold(vec4 z) {
    if (z == vec4(0.0)) return z;
    else return cmult(complex(clampd(inv(cmodsqr(z)), 1.0, 4.0)), z);
}

vec2 cabs(vec2 z) {
    return abs(z);
}
vec4 cabs(vec4 z) {
    return vec4(absd(z.xy), absd(z.zw));
}

vec2 cabsr(vec2 z) {
    return vec2(abs(z.x), z.y);
}
vec4 cabsr(vec4 z) {
    return vec4(absd(z.xy), z.zw);
}

vec2 cabsi(vec2 z) {
    return vec2(z.x, abs(z.y));
}
vec4 cabsi(vec4 z) {
    return vec4(z.xy, absd(z.zw));
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
    return cmult(a, vec2(cos(phi), sin(phi)));
}
vec4 rotate(vec4 z, float phi) {
    return cmult(z, vec2(cos(phi), sin(phi)));
}

vec2 rotate(vec2 a, float sinPhi, float cosPhi) {
    return vec2(a.x*cosPhi - a.y*sinPhi, a.x*sinPhi + a.y*cosPhi);
}

vec2 rotate(vec2 p, vec2 q, float phi) {
    return rotate(p - q, phi) + q;
}
vec4 rotate(vec4 p, vec4 q, float phi) {
    return cadd(rotate(cadd(p, -q), phi), q);
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
vec4 polar(vec4 z) { return vec4(cmod(z), carg(z)); }

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
        add(z.zw, carg(vec4(mult(w.xy, sint), add(z.xy, mult(w.xy, cost)))))
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

    if (s.zw == _0 && s.xy == vec2(2.0, 0.0)) return csqr_polar(z);
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





vec2 delta(vec2 z, vec2 ze, vec2 alpha) {
    return cmult(alpha, (z - ze)/0.0001);
}
vec2 delta(vec4 z, vec4 ze, vec2 alpha) {
    return cmult(alpha, cdiv(cadd(z, -ze), 0.0001)).xz;
}

vec2 testshape(vec2 z, vec2 c) {

    // return cmult(c, z - ctan(z));
    // return eta(z) + c;
    // return cmult(z, cexp(-z)) + c;

    return cdiv(cquad(z) - vec2(2.0, 0.0), 2.0*ccube(z) - z) + c;

    //return cmult(cinv(c), gamma(z));
    // j = 1.90825873 + 2.49050274i
}
vec4 testshape(vec4 z, vec4 c) { return vec4(0.0); }

vec2 cephalopod(vec2 z, vec2 c) { return cmult(c, ctan(z)); }
vec4 cephalopod(vec4 z, vec4 c) { return cmult(c, ctan(z)); }

vec2 mandelbrot(vec2 z, vec2 c) { return csqr(z) + c; }
vec4 mandelbrot(vec4 z, vec4 c) { return cadd(csqr(z), c); }
vec2 mandelbrot_delta1(vec2 alpha, vec2 z1) {
    return power*cmult(cpow(z1, int(power) - 1), alpha) + _1;
}
vec2 mandelbrot_delta1(vec2 alpha, vec4 z1) {
    return mandelbrot_delta1(alpha, z1.xz);
}
vec2 mandelbrot_delta2(vec2 beta, vec2 alpha, vec2 z1) {
    vec2 p = _1;
    for (int i = 0; i < int(power) - 2; i++) { p = cmult(p, z1); }
    vec2 q = cmult(p, z1);
    return power*((power - 1.0)*cmult(p, csqr(alpha)) + cmult(q, beta));
}
vec2 mandelbrot_delta2(vec2 beta, vec2 alpha, vec4 z1) {
    return mandelbrot_delta2(beta, alpha, z1.xz);
}
vec2 mandelbrot_julia_delta1(vec2 alpha, vec2 z1) {
    return power*cmult(cpow(z1, int(power) - 1), alpha);
}
vec2 mandelbrot_julia_delta1(vec2 alpha, vec4 z1) {
    return mandelbrot_julia_delta1(alpha, z1.xz);
}

vec2 mandelbrot_cubic(vec2 z, vec2 c) { return ccube(z) + c; }
vec4 mandelbrot_cubic(vec4 z, vec4 c) { return cadd(ccube(z), c); }

vec2 mandelbrot_quartic(vec2 z, vec2 c) { return cquad(z) + c; }
vec4 mandelbrot_quartic(vec4 z, vec4 c) { return cadd(cquad(z), c); }

vec2 mandelbrot_quintic(vec2 z, vec2 c) { return cquint(z) + c; }
vec4 mandelbrot_quintic(vec4 z, vec4 c) { return cadd(cquint(z), c); }

vec2 mandelbrot_sextic(vec2 z, vec2 c) { return csext(z) + c; }
vec4 mandelbrot_sextic(vec4 z, vec4 c) { return cadd(csext(z), c); }

vec2 mandelbrot_power(vec2 z, vec2 c) { return cpow(z, p1) + c; }
vec4 mandelbrot_power(vec4 z, vec4 c) { return cadd(cpow(z, p1), c); }

vec2 clover(vec2 z, vec2 c) {
    vec2 q = cpow(z, int(floor(p1.x)));
    return cmult(c, q + cinv(q));
}
vec4 clover(vec4 z, vec4 c) {
    vec4 q = cpow(z, int(floor(p1.x)));
    return cmult(c, cadd(q, cinv(q)));
}

vec2 mandelbox(vec2 z, vec2 c) {
    float t;
    if (p3.x > 1.0) t = 0.1;
    else t = 1.0;
    return cpow(cmult(p1, ballfold(t*p2.x*boxfold(z))), int(p3.x)) - c;
}
vec4 mandelbox(vec4 z, vec4 c) {
    float t;
    if (p3.x > 1.0) t = 0.1;
    else t = 1.0;
    return cadd(cpow(cmult(p1, ballfold(cmult(t*p2.x, boxfold(z)))), int(p3.x)), -c);
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
vec4 kali_square(vec4 z, vec4 c) { return cadd(cdiv(vec4(absd(z.xy), absd(z.zw)), mult(z.xy, z.zw)), c); }

vec2 mandelbar(vec2 z, vec2 c) { return csqr(conj(z)) + c; }
vec4 mandelbar(vec4 z, vec4 c) { return cadd(csqr(conj(z)), c); }

vec2 burning_ship(vec2 z, vec2 c) { return csqr(abs(z)) + c; }
vec4 burning_ship(vec4 z, vec4 c) { return cadd(csqr(vec4(absd(z.xy), absd(z.zw))), c); }

vec2 burning_ship_power(vec2 z, vec2 c) {
    return cpow(cabs(z), p1) + c;
}
vec4 burning_ship_power(vec4 z, vec4 c) {
    return cadd(cpow(cabs(z), p1), c);
}

vec2 sine(vec2 z, vec2 c) { return csin(z) + c; }
vec4 sine(vec4 z, vec4 c) { return cadd(csin(z), c); }

vec2 cosine(vec2 z, vec2 c) { return ccos(z) + c; }
vec4 cosine(vec4 z, vec4 c) { return cadd(ccos(z), c); }

vec2 hyperbolic_sine(vec2 z, vec2 c) { return -cmult(_i, csinh(z)) + c; }
vec4 hyperbolic_sine(vec4 z, vec4 c) { return cadd(-cmult(_i, csinh(z)), c); }

vec2 hyperbolic_cosine(vec2 z, vec2 c) { return ccosh(z) + c; }
vec4 hyperbolic_cosine(vec4 z, vec4 c) { return cadd(ccosh(z), c); }

vec2 bessel(vec2 z, vec2 c) { return bessel(z) + c; }
vec4 bessel(vec4 z, vec4 c) { return cadd(bessel(z), c); }

vec2 sine2(vec2 z, vec2 c) { return csin(cdiv(csqr(z) + p1, c)); }
vec4 sine2(vec4 z, vec4 c) { return csin(cdiv(cadd(csqr(z), p1), c)); }

vec2 horseshoe_crab(vec2 z, vec2 c) { return csin(cdiv(p1, z) + cdiv(z, c)); }
vec4 horseshoe_crab(vec4 z, vec4 c) { return csin(cadd(cdiv(p1, z), cdiv(z, c))); }

void kleinian_init(inout vec2 z, vec2 c, inout vec2 alpha, out vec2 t, out float K, out float M, out float k) {

    t = vec2(p1.x, mod(p1.y - 1.0, 2.0) - 1.0);
    // vec2 z0 = z;

    vec2 q = z - c;
    vec2 qe = q + vec2(0.0001, 0.0);
    float modsqrq = cmodsqr(q);
    float modsqrqe = cmodsqr(qe);

    q /= cmodsqr(q);
    qe /= cmodsqr(qe);
    z = q + c;
    vec2 ze = qe + c;
    alpha = delta(z, ze, alpha);

    if (p2.x == 0.0) k = 2.0; else k = 2.0*cos(pi/p2.x);

    if (cmod(vec2(t.x, abs(t.y)) - vec2(sqrt(3.0), 1.0)) > 0.275) {

        K = 0.245;
        float s = pow(abs(1.666667*t.y), 0.3);
        M = (1.0 - s)*10.0 + s*0.75;

    }

    // developer only
    if (p3.x != 0.0 || p4.x != 0.0) {
        K *= p3.x;
        M *= p4.x;
    }

//    float L = 1.0;
//    if (p2.x == 3.0) L = cos(pi/(k + t.y)*(z0.x + 0.5*t.y));
//    if (abs(z0.y - (0.5*t.x - sign(t.y)*sign(z0.x - 0.5*t.y)*K*t.x*(1.0 - exp(-M*abs(z0.x*L - 0.5*t.y))))) < 0.005) {
//        z.y = -1.0;
//    }

}
void kleinian_init(inout vec4 z, vec4 c, inout vec2 alpha, out vec2 t, out float K, out float M, out float k) {

    t = vec2(p1.x, mod(p1.y - 1.0, 2.0) - 1.0);
    // vec4 z0 = z;

    // vec2 modsqrz = cmodsqr(z);
    vec4 q = cadd(z, -c);
    vec4 qe = cadd(q, vec4(0.0001, 0.0, 0.0, 0.0));
    vec2 modsqrq = cmodsqr(q);
    vec2 modsqrqe = cmodsqr(qe);

    q = cdiv(q, complex(cmodsqr(q)));
    qe = cdiv(qe, complex(cmodsqr(qe)));
    z = cadd(q, c);
    vec4 ze = cadd(qe, c);
    alpha = delta(z, ze, alpha);


    if (p2.x == 0.0) k = 2.0; else k = 2.0*cos(pi/p2.x);

    if (cmod(vec2(t.x, abs(t.y)) - vec2(sqrt(3.0), 1.0)) > 0.275) {

        K = 0.245;
        float s = pow(abs(1.666667*t.y), 0.3);
        M = (1.0 - s)*10.0 + s*0.75;

    }

    // developer only
    if (p3.x != 0.0 || p4.x != 0.0) {
        K *= p3.x;
        M *= p4.x;
    }

//    float L = 1.0;
//    if (p2.x == 3.0) L = cos(pi/(k + t.y)*(z0.x + 0.5*t.y));
//    if (abs(z0.z - (0.5*t.x - sign(t.y)*sign(z0.x - 0.5*t.y)*K*t.x*(1.0 - exp(-M*abs(z0.x*L - 0.5*t.y))))) < 0.005) {
//        z.z = -1.0;
//    }

}
vec2 kleinian(vec2 z, vec2 c, vec2 t, float K, float M, float k) {

    if (z.y < 0.0 || z.y > t.x) return z;

    vec2 w = z;

    if      (t.y*z.y > abs(t.x)*(  0.5*k - z.x )) w.x -= k;
    else if (t.y*z.y < abs(t.x)*( -0.5*k - z.x )) w.x += k;
    else {
        float L = 1.0;
        if (p2.x == 3.0) L = cos(3.0*(z.x + 0.5*t.y));
        if (z.y < 0.5*t.x + sign(t.y)*sign(z.x + 0.5*t.y)*K*t.x*(1.0 - exp(-M*abs(z.x*L + 0.5*t.y)))) {
            w = cmulti(t) + cinv(z);  // it + 1/z
        }
        else {
            w = cinv(z - cmulti(t));  // 1/(z - it)
        }
    }
    return w;

}
vec4 kleinian(vec4 z, vec4 c, vec2 t, float K, float M, float k) {

    if (ltd(z.zw, 0.0) || gtd(z.zw, t.x)) return z;

    vec4 w = z;

    if      (gtd(add(mult(t.y, z.zw), mult(abs(t.x), z.xy)), abs(t.x)*0.5*k)) w.xy = add(w.xy, -k);
    else if (ltd(add(mult(t.y, z.zw), mult(abs(t.x), z.xy)), abs(t.x)*-0.5*k)) w.xy = add(w.xy, k);
    else {
        float L = 1.0;
        if (p2.x == 3.0) L = cos(3.0*(z.x + 0.5*t.y));
        if (ltd(z.zw, 0.5*t.x + sign(t.y)*sign(z.x + 0.5*t.y)*K*t.x*(1.0 - exp(-M*abs(z.x*L + 0.5*t.y))))) {
            w = cadd(cmulti(t), cinv(z));  // it + 1/z
        }
        else {
            w = cinv(cadd(z, -cmulti(t)));  // 1/(z - it)
        }
    }
    return w;

}
bool kleinian_exit(vec2 z, vec2 t) {
    if (z.y < 0.0 || z.y > t.x) return true; else return false;
}
bool kleinian_exit(vec4 z, vec2 t) {
    if (ltd(z.zw, 0.0) || gtd(z.zw, t.x)) return true;
    else return false;
}
vec2 kleinian_delta(vec2 alpha, vec2 z, vec2 t, float K, float M, float k) {

    if (z.y > t.x || z.y < 0.0) return alpha;

    vec2 w = z;

    if      (t.y*z.y > abs(t.x)*(  0.5*k - z.x )) return alpha;
    else if (t.y*z.y < abs(t.x)*( -0.5*k - z.x )) return alpha;
    else {
        float L = 1.0;
        if (p2.x == 3.0) L = cos(3.0*(z.x + 0.5*t.y));
        if (z.y < 0.5*t.x + sign(t.y)*sign(z.x + 0.5*t.y)*K*t.x*(1.0 - exp(-M*abs(z.x*L + 0.5*t.y)))) {
            if (z == _0) return _0; else return -cdiv(alpha, csqr(z));
        }
        else {
            if (z == cmulti(t)) return _0; return cdiv(alpha, csqr(cmulti(z) + t));
        }
    }

}
vec2 kleinian_delta(vec2 alpha, vec4 z, vec2 t, float K, float M, float k) {
    return kleinian_delta(alpha, z.xz, t, K, M, k);
}

vec2 necklace(vec2 z, vec2 c) { return z - cmult(c, (z + cinv(cpow(z, int(floor(p1.x)) - 1)))/floor(p1.x)); }
vec4 necklace(vec4 z, vec4 c) { return cadd(z, -cmult(c, cdiv(cadd(z, cinv(cpow(z, int(floor(p1.x)) - 1))), floor(p1.x)))); }

vec2 magnet1(vec2 z, vec2 c) {
    return csqr(cdiv(csqr(z) + c - complex(p1.x), 2.0*z + c - complex(p1.y)));
}
vec4 magnet1(vec4 z, vec4 c) {
    return csqr(cdiv(cadd(cadd(csqr(z), c), -p1.x), cadd(2.0*z, cadd(c, -p1.y))));
}

vec2 magnet2(vec2 z, vec2 c) {
    vec2 a = c - complex(p1.x);
    vec2 b = c - complex(p1.y);
    vec2 ab = cmult(a, b);
    return csqr(cdiv(((ccube(z) + cmult(3.0*a, z)) + ab), (((3.0*csqr(z) + cmult(3.0*b, z)) + ab) + _1)));
}
vec4 magnet2(vec4 z, vec4 c) {
    vec4 a = cadd(c, -p1.x);
    vec4 b = cadd(c, -p1.y);
    vec4 ab = cmult(a, b);
    return csqr(cdiv(cadd(cadd(ccube(z), cmult(cmult(3.0, a), z)), ab), cadd(cadd(cadd(cmult(3.0, csqr(z)), cmult(cmult(3.0, b), z)), ab), 1.0)));
}

vec2 nova1(vec2 z, vec2 c) {
    vec2 zsqr = csqr(z);
    return z - cmult(p1, cdiv(cmult(z, zsqr) - _1, 3.0*zsqr)) + c;
}
vec4 nova1(vec4 z, vec4 c) {
    vec4 zsqr = csqr(z);
    return cadd(cadd(z, -cmult(vec4(p1.x, 0.0, p1.y, 0.0), cdiv(cadd(cmult(z, zsqr), -vec4(_1, _0)), cmult(vec4(3.0*_1, _0), zsqr)))), c);
}

vec2 nova2(vec2 z, vec2 c) {
    vec2 zSqr = csqr(z);
    return z - cdiv(csin(zSqr) - z, 2.0*cmult(z, ccos(zSqr)) - _1) + c;
}
vec4 nova2(vec4 z, vec4 c) {
    vec4 zsqr = csqr(z);
    return cadd(cadd(z, -cdiv(cadd(csin(zsqr), -z), cadd(2.0*cmult(z, ccos(zsqr)), -1.0))), c);
}

vec2 collatz(vec2 z, vec2 c) {
    return 0.25*(2.0*_1 + 7.0*z - cmult((2.0*_1 + 5.0*z), ccos(pi*z))) + c;
}
vec4 collatz(vec4 z, vec4 c) {
    return cadd(0.25*cadd(cadd(2.0, cmult(7.0, z)), -cmult(cadd(2.0, cmult(5.0, z)), ccos(cmult(pi, z)))), c);
}

vec2 mandelex(vec2 z, vec2 c) {
    vec2 w = linearpull(z, p4.x, p4.x);
    if (abs(c.x) < 2.0 && abs(c.y) < 2.0) { w = nonlinearpull(w, 2.0, 2.0); }
    return linearpull(rotate(circinv(wrapbox(w, 0.5, 0.5), p2.x)*p3.x, p1.x) + c, p4.x, p4.x);
}
vec4 mandelex(vec4 z, vec4 c) {
    vec4 w = linearpull(z, p4.x, p4.x);
    if (abs(c.x) < 2.0 && abs(c.z) < 2.0) { w = nonlinearpull(w, 2.0, 2.0); }
    return linearpull(cadd(rotate(cmult(vec4(p3.x, 0.0, 0.0, 0.0), circinv(wrapbox(w, 0.5, 0.5), p2.x)), p1.x), c), p4.x, p4.x);
}

vec2 binet(vec2 z, vec2 c) {
    return (cpow(phi, z) - cpow(-1.0/phi, z))/sqrt(5.0) + c;
}
vec4 binet(vec4 z, vec4 c) {
    return cadd(cdiv(cadd(cpow(vec2(phi, 0.0), z), -cpow(vec2(-1.0/phi, 0.0), z)), sqrt(5.0)), c);
}

vec2 cactus(vec2 z, vec2 c) {
    return ccube(z) + cmult(c - _1, z) - c;
}
vec4 cactus(vec4 z, vec4 c) {
    return cadd(ccube(z), cadd(cmult(cadd(c, -_1), z), -c));
}

vec2 sierpinski_tri(vec2 z, vec2 c) {

    float h = 0.5;
    float q = sqrt(3.0)/3.0;

    vec2 w = z;

    if (z.y >= 0.0) {
        w.y -= h;
        w = rotate(w, 2.0*vec2(0.0, h), p2.x);
    }
    else {
        if (z.x > 0.0) {
            w = rotate(w, 2.0*vec2(q, -h), p2.x);
            w.x -= q;
        }
        else {
            w = rotate(w, 2.0*vec2(-q, -h), p2.x);
            w.x += q;
        }
        w.y += h;
    }

    return 2.0*p1.x*w + c;

}
vec4 sierpinski_tri(vec4 z, vec4 c) {

    float h = 0.5;
    float q = sqrt(3.0)/3.0;

    vec4 w = z;

    if (z.zw == _0 || z.z > 0.0 || (z.z == 0.0 && z.w > 0.0)) {
        w.zw = add(w.zw, _double(-h));
        w = rotate(w, _double(2.0*vec2(0.0, h)), p1.x);
    }
    else {
        if (gtd(z.xy, 0.0)) {
            w = rotate(w, _double(2.0*vec2(q, -h)), p1.x);
            w.xy = add(w.xy, _double(-q));
        }
        else {
            w = rotate(w, _double(2.0*vec2(-q, -h)), p1.x);
            w.xy = add(w.xy, _double(q));
        }
        w.zw = add(w.zw, _double(h));
    }

    return cadd(cmult(2.0*p1.y, w), c);

}

vec2 sierpinski_sqr(vec2 z, vec2 c) {

    float t = 1.0/3.0;
    float s = 1.0/6.0;

    vec2 w = z;
    if (z.x < -s) {
        w.x += t;
        if (z.y < -s) w.y += t;
        else if (z.y > s) w.y -= t;
    }
    else if (z.x < s) {
        if (z.y > 0.0) w.y -= t;
        else w.y += t;
    }
    else {
        w.x -= t;
        if (z.y < -s) w.y += t;
        else if (z.y > s) w.y -= t;
    }

    return 3.0*w + c;

}
vec4 sierpinski_sqr(vec4 z, vec4 c) {

    float t = 1.0/3.0;
    float s = 1.0/6.0;

    vec4 w = z;
    if (ltd(z.xy, -s)) {
        w.xy = add(w.xy, t);
        if (ltd(z.zw, -s)) w.zw = add(w.zw, t);
        else if (gtd(z.zw, s)) w.zw = add(w.zw, -t);
    }
    else if (ltd(z.xy, s)) {
        if (gtd(z.zw, 0.0)) w.zw = add(w.zw, -t);
        else w.zw = add(w.zw, t);
    }
    else {
        w.xy = add(w.xy, -t);
        if (ltd(z.zw, -s)) w.zw = add(w.zw, t);
        else if (gtd(z.zw, s)) w.zw = add(w.zw, -t);
    }

    return cadd(cmult(3.0, w), c);

}

vec2 sierpinski_pent(vec2 z, vec2 c) {

    // julia = -1.25852935i
    // center = 1.5
    // scale = 1.68

    float pi5 = pi/5.0;
    float delta = 2.0*pi5;
    float phi1 = 3.0*pi/10.0;
    float phi2 = phi1 + delta;
    float phi3 = phi1 - delta;
    float phi4 = phi3 - delta;
    float phi5 = phi4 - delta;

    float theta = carg(z);
    vec2 w = z;
    float dir;
    float r = p1.x;                 // center radius
    float s = 2.0*r*sin(pi/5.0);    // side length
    float t = 0.5*s/tan(pi/5.0);    // distance to edge

    float tanpi5 =  0.726542528;
    float tan2pi5 = 3.077683537;
    if (z.y < tanpi5*z.x + r &&
    z.y < -tanpi5*z.x + r &&
    z.y > tan2pi5*(z.x - r*cos(pi/10.0)) + r*sin(pi/10.0) &&
    z.y > -tan2pi5*(z.x - r*cos(-7.0*pi/10.0)) + r*sin(-7.0*pi/10.0) &&
    z.y > -t) {

        w.y = -w.y;

    }
    else {
        if (abs(theta - phi1) < pi5) dir = phi1;
        else if (abs(theta - phi2) < pi5) dir = phi2;
        else if (abs(theta - phi3) < pi5) dir = phi3;
        else if (abs(theta - phi4) < pi5) dir = phi4;
        else dir = phi5;
        w -= phi*vec2(cos(dir), sin(dir));
    }

    w = p2.x*w + c;
    return rotate(w, p3.x);

}
vec4 sierpinski_pent(vec4 z, vec4 c) {

    // scale = 2.82
    // texture: orbit trap high val
    // position rotation = -135

    // julia = -1.25852935i
    // center = 1.5
    // scale = 1.68

    float pi5 = pi/5.0;
    float delta = 2.0*pi5;
    float phi1 = 3.0*pi/10.0;
    float phi2 = phi1 + delta;
    float phi3 = phi1 - delta;
    float phi4 = phi3 - delta;
    float phi5 = phi4 - delta;

    float theta = atan(z.z, z.x);
    vec4 w = z;
    float dir;
    float r = p1.x;                 // center radius
    float s = 2.0*r*sin(pi/5.0);    // side length
    float t = 0.5*s/tan(pi/5.0);    // distance to edge

    float tanpi5 =  0.726542528;
    float tan2pi5 = 3.077683537;
    if (z.z < tanpi5*z.x + r &&
        z.z < -tanpi5*z.x + r &&
        z.z > tan2pi5*(z.x - r*cos(pi/10.0)) + r*sin(pi/10.0) &&
        z.z > -tan2pi5*(z.x - r*cos(-7.0*pi/10.0)) + r*sin(-7.0*pi/10.0) &&
        z.z > -t) {

            w.zw = -w.zw;

        }
    else {
        if (abs(theta - phi1) < pi5) dir = phi1;
        else if (abs(theta - phi2) < pi5) dir = phi2;
        else if (abs(theta - phi3) < pi5) dir = phi3;
        else if (abs(theta - phi4) < pi5) dir = phi4;
        else dir = phi5;
        w = cadd(w, -phi*vec2(cos(dir), sin(dir)));
    }

    w = cadd(cmult(p2.x, w), c);
    return rotate(w, p3.x);

}

vec2 dragon(vec2 z, vec2 c) {

    // if (z.y < z.x && z.y < 1.0 - z.x && z.y > 0.0) return z;
    if (z.y < z.x && z.x < 0.5 && z.y > 0.0) return vec2(0.5 - z.x, z.y);

    vec2 w = z;
    if (z.x > p1.x) {
        w = p1.y*rotate(w - _1, -_3pi4.x);
        //w = p1.y*cdiv(ONE - z, ONE - I);
    }
    else {
        w = p1.y*rotate(w, -_pi4.x);
        //w = p1.y*cdiv(z, ONE + I);
    }
    return w + c;

}

vec2 tsquare(vec2 z, vec2 c) {

    // shift = 1.0
    // scale = 1.71
    // rotate = 45

    vec2 root = rotate(z, p3.x);
    if (abs(root.x) < 0.5 && abs(root.y) < 0.5) return R/z;

    vec2 w = z;
    if (z.x < 0.0) {
        w.x += p1.x;
        if (z.y < 0.0) w.y += p1.x;
        else w.y -= p1.x;
    }
    else {
        w.x -= p1.x;
        if (z.y < 0.0) w.y += p1.x;
        else w.y -= p1.x;
    }

    w *= p2.x;

    return rotate(w, p3.x) + c;

}
vec4 tsquare(vec4 z, vec4 c) {

    // shift = 1.0
    // scale = 1.71
    // rotate = 45

    vec4 root = rotate(z, p3.x);
    if (abs(root.x) < 0.5 && abs(root.z) < 0.5) return cdiv(R, z);

    vec4 w = z;
    if (ltd(z.xy, 0.0)) {
        w.xy = add(w.xy, p1.x);
        if (ltd(z.zw, 0.0)) w.zw = add(w.zw, p1.x);
        else w.zw = add(w.zw, -p1.x);
    }
    else {
        w.xy = add(w.xy, -p1.x);
        if (ltd(z.zw, 0.0)) w.zw = add(w.zw, p1.x);
        else w.zw = add(w.zw, -p1.x);
    }

    w = cmult(p2.x, w);

    return cadd(rotate(w, p3.x), c);

}

vec2 lambert_newton(vec2 z, vec2 c) {
    vec2 expz = cexp(z);
    return z - cdiv(cmult(z, expz) + c, cmult(z + _1, expz));
}
vec4 lambert_newton(vec4 z, vec4 c) {
    vec4 expz = cexp(z);
    return cadd(z, -cdiv(cadd(cmult(z, expz), c), cmult(cadd(z, _1), expz)));
}

vec2 taurus(vec2 z, vec2 c) {
    vec2 zsqr = csqr(z);
    vec2 zcube = cmult(z, zsqr);
    return cdiv(zcube + zsqr + _1, 2.0*zsqr - c + _1);
}
vec4 taurus(vec4 z, vec4 c) {
    vec4 zsqr = csqr(z);
    vec4 zcube = cmult(z, zsqr);
    return cdiv(cadd(zcube, cadd(zsqr, 1.0)), cadd(2.0*zsqr, cadd(1.0, -c)));
}

vec2 ammonite(vec2 z, vec2 c) {
    return csinh(clog(cabsi(csqr(z + c) - _1)));
}
vec4 ammonite(vec4 z, vec4 c) {
    return csinh(clog(cabsi(cadd(csqr(cadd(z, c)), -1.0))));
}

vec2 phoenix(vec2 z1, vec2 z2, vec2 c) {
    return cpow(z1, int(p1.x)) + cmult(c, cpow(z1, int(p2.x))) + cmult(p3, z2);
}

vec2 ballfold1(vec2 z, vec2 c) {
    return ballfold(csqr(z) + p1) + c;
}
vec4 ballfold1(vec4 z, vec4 c) {
    return cadd(ballfold(cadd(csqr(z), p1)), c);
}

// customShapeHandleSingle
// customShapeHandleDual




float divergence(vec2 z, vec2 z1, out float modz1, bool textureIn) {

    float modz = cmod2(z);
    modz1 = cmod2(z1);
    float div;
    //    float div = log(modz)/log(modz1);

    if (isSpecial(z.x) || isSpecial(z.y))   div = log(1e38)/log(modz1);
    else if (isSpecial(modz))               div = 0.5*log(1e38)/log(modz1);
    else if (textureIn)                     div = power;
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
    else if (textureIn)                     div = power;
    else                                    div = log(modz.x)/log(modz1.x);

    return div;

}

float iteration_final(uint n) {
    float j = float(n);
    return j/75.0;
}

void exp_smoothing_loop(inout float sum, float modsqrz) {
    sum += exp(-modsqrz);
}
void exp_smoothing_loop(inout vec2 sum, vec2 modsqrz) {
    sum.x += exp(-modsqrz.x);
}
float exp_smoothing_final(float sum) {
    return sum/75.0;
}
float exp_smoothing_final(vec2 sum) {
    return exp_smoothing_final(sum.x);
}

float escape_smooth_final(uint n, vec2 z, vec2 z1, bool textureIn) {
    float modz1;
    float div = divergence(z, z1, modz1, textureIn);
    float i = float(n) - log(log(modz1)/log(R))/log(div);
    return i/75.0;
}
float escape_smooth_final(uint n, vec4 z, vec4 z1, bool textureIn) {
    vec2 modz1;
    float div = divergence(z, z1, modz1, textureIn);
    float i = float(n) - log(log(modz1.x)/log(R))/log(div);
    return i/75.0;
}

void converge_smooth_loop(inout float sum, vec2 z, vec2 z1) {
    sum += exp(-1.0/cmod(z1 - z));
}
void converge_smooth_loop(inout vec2 sum, vec4 z, vec4 z1) {
    sum.x += exp(-1.0/cmod(z1.xz - z.xz));
}
float converge_smooth_final(float sum, uint n) {
    return sum/75.0;
}
float converge_smooth_final(vec2 sum, uint n) {
    return sum.x/75.0;
}

float dist_estim_final(float modsqrz, vec2 alpha) {
    return sqrt(modsqrz)*0.5*log(modsqrz)/cmod(alpha)/xScale.x;
}
float dist_estim_final(vec2 modsqrz, vec2 alpha) {
    return dist_estim_final(modsqrz.x, alpha);
}

float dist_estim_abs_final(float modsqrz, vec2 alpha) {
    float d = sqrt(modsqrz)*0.5*log(modsqrz)/cmod(alpha);
    if (d*200.0/xScale.x > log(q1.x + 1.0)) return 1.5;
    else return 0.0;
}
float dist_estim_abs_final(vec2 modsqrz, vec2 alpha) {
    return dist_estim_abs_final(modsqrz.x, alpha);
}

void orbit_trap_minx_loop(inout float minx, vec2 z) {
    float dist = abs(z.x);
    if (dist < minx) { minx = dist; }
}
void orbit_trap_minx_loop(inout float minx, vec4 z) {
    float dist = abs(z.x);
    if (dist < minx) { minx = dist; }
}

void orbit_trap_miny_loop(inout float miny, vec2 z) {
    float dist = abs(z.y);
    if (dist < miny) { miny = dist; }
}
void orbit_trap_miny_loop(inout float miny, vec4 z) {
    float dist = abs(z.y);
    if (dist < miny) { miny = dist; }
}

void orbit_trap_circ_loop(vec2 z, inout float minDist) {
    // float xdist = z.x - q1.x;
    // float ydist = z.y - q1.y;
    // float dist = sqrt(q2.x*xdist*xdist + (1.0 - q2.x)*ydist*ydist);
    float dist = abs(cmod(z - q1) - q2.x);
    if (dist < minDist) { minDist = dist; }
}
void orbit_trap_circ_loop(vec4 z, inout float minDist) {
    orbit_trap_circ_loop(z.xz, minDist);
}

void orbit_trap_line_loop(vec2 z, inout float minDist) {
    // q1.x :: distance from origin
    // q2.x :: rotation
    float m = tan(q2.x);
    vec2 center = rotate(vec2(0.0, q1.x), q2.x);
    float dist = abs(abs(-m*z.x + z.y + m*center.x - center.y)*cos(q2.x));
    if (dist < minDist) minDist = dist;
}
void orbit_trap_line_loop(vec4 z, inout float minDist) {
    orbit_trap_line_loop(z.xz, minDist);
}

void orbit_trap_box_loop(vec2 z, inout float minDist) {
    // q1 :: center
    // q2 :: (width, height)
    float dist;
    float d1 = q1.x - q2.x - z.x;
    float d2 = z.x - (q1.x + q2.x);
    float d3 = q1.y - q2.y - z.y;
    float d4 = z.y - (q1.y + q2.y);

    if (z.x > q1.x + q2.x || z.x < q1.x - q2.x || z.y > q1.y + q2.y || z.y < q1.y - q2.y) {
        // outside box
        vec2 d = vec2(
            max(d1, max(0.0, d2)),
            max(d3, max(0.0, d4))
        );
        dist = cmod(d);
    }
    else {
        dist = min(min(abs(d1), abs(d2)), min(abs(d3), abs(d4)));
    }

    if (dist < minDist) minDist = dist;
}
void orbit_trap_box_loop(vec4 z, inout float minDist) {
    orbit_trap_box_loop(z.xz, minDist);
}

void orbit_trap_circ_puncture_loop(vec2 z, inout float minDist, inout float angle) {
    float dist = cmod(z - q1);
    if (abs(dist - q2.x) < minDist)  {
        minDist = dist;
        angle = carg(z - q1);
    }
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
    sum += pow(0.5*(sin(q1.x*argz + q2.x) + 1.0), q3.x);
}
void stripe_avg_loop(inout vec2 sum, inout vec2 sum1, vec4 z) {
    sum1 = sum;
    float argz = atan(z.z, z.x);
    sum.x += pow(0.5*(sin(q1.x*argz + q2.x) + 1.0), q3.x);
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
        vec2 modpowz2 = pow(modsqrz2.x, 0.5*power)*_1;
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
        vec2 w = cmult(z - z1, cpow(z1 - z2, vec2(-1.0, q2.x)));
        float argz = carg(w);
        sum += pow(abs(argz)/pi, q1.x);
    }
}
void curvature_avg_loop(inout vec2 sum, inout vec2 sum1, uint n, vec4 z, vec4 z1, vec4 z2) {
    if (n > 1u) {
        sum1.x = sum.x;
        vec2 w = cmult(vec2(z.x - z1.x, z.z - z1.z), cpow(vec2(z1.x - z2.x, z1.z - z2.z), vec2(-1.0, q2.x)));
        sum.x += pow(abs(atan(w.y, w.x))/pi, q1.x);
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

void curvature_avg2_loop(inout float sum, inout float sum1, uint n, vec2 z, vec2 z1, vec2 z2, vec2 z3) {
    if (n > 3u) {
        sum1 = sum;
        vec2 v = z - z1;
        vec2 v1 = z1 - z2;
        vec2 v2 = z2 - z3;
        vec2 v3 = z3;
        vec2 w = rotate(cdiv(cmult(v, v2), cmult(v1, v3)), q1.x);
        sum += abs(carg(w))/pi;
    }
}

void overlay_avg_loop(inout float sum, inout float sum1, vec2 z) {
    sum1 = sum;
    float argz = atan(z.y, z.x);
    sum += 0.5*(tan(q1.x*argz)/tan(q1.x*pi) + 1.0);
}
void overlay_avg_loop(inout vec2 sum, inout vec2 sum1, vec4 z) {
    sum1.x = sum.x;
    float argz = atan(z.z, z.x);
    sum.x += 0.5*(tan(q1.x*argz)/tan(q1.x*pi) + 1.0);
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

void angular_momentum_loop(inout float sum, inout float sum1, vec2 z, vec2 z1, vec2 z2) {

    sum1 = sum;

    vec2 v = z1 - z2;
    float r = cmod(z);
    float phi = carg(v) - carg(z);
    float vperp = dot(cmult(_i, z)/r, v);
    float vang = vperp/r;

    sum += vang;

}
void angular_momentum_loop(inout vec2 sum, inout vec2 sum1, vec4 z, vec4 z1, vec4 z2) {
    angular_momentum_loop(sum.x, sum1.x, z.xz, z1.xz, z2.xz);
}

void umbrella_loop(inout float sum, inout float sum1, vec2 z, vec2 z1) {

    sum1 = sum;
    vec2 w = cpow(cmult(z, conj(z1)), -q1.x);
    float t = acos(clamp(w.x, -1.0, 1.0));
    sum += 0.5*(t + 1.0);

}
void umbrella_loop(inout vec2 sum, inout vec2 sum1, vec4 z, vec4 z1) {

    sum1 = sum;
    vec2 w = cpow(cmult(z.xz, conj(z1.xz)), -q1.x);
    float t = acos(clamp(w.x, -1.0, 1.0));
    sum.x += 0.5*(t + 1.0);

}

void umbrella_inverse_loop(inout float sum, inout float sum1, vec2 z, vec2 z1) {

    sum1 = sum;
    vec2 w = cpow(cmult(z, conj(z1)), q1.x);
    float t = acos(clamp(w.x, -1.0, 1.0));
    sum += 0.5*(t + 1.0);

}
void umbrella_inverse_loop(inout vec2 sum, inout vec2 sum1, vec4 z, vec4 z1) {

    sum1 = sum;
    vec2 w = cpow(cmult(z.xz, conj(z1.xz)), q1.x);
    float t = acos(clamp(w.x, -1.0, 1.0));
    sum.x += 0.5*(t + 1.0);

}

void exit_angle_loop(inout float sum, inout float sum1, vec2 z, vec2 z1) {
    if (z.x > 0.0 && z1.x < 0.0 || z.x < 0.0 && z1.x > 0.0) sum += 1.0;
}
float exit_angle_final(vec2 z, vec2 z1) {

    float modz1;
    float div = divergence(z, z1, modz1, false);
    float t = -log(log(modz1)/log(R))/log(div);
    float s = (1.0 - t)*carg(z1) + t*carg(z);

    return pow(cmod(z) - R, q1.x);
}

float angle_final(vec2 c) {
    return carg(c - vec2(xCoord.x, yCoord.x) - q1);
}
float angle_final(vec4 c) {
    return carg(cadd(cadd(c, -vec4(xCoord, yCoord)), -q1)).x;
}

void star_lens_loop(inout float sum, inout float sum1, vec2 z) {
    sum1 = sum;
    vec2 w = rotate(z, q2.x) - q3;
    sum += 1.0/(1.0 + pow(abs((w.x*w.x - w.y*w.y)/q1.x), exp(q4.x - 1.0)));
}
void star_lens_loop(inout vec2 sum, inout vec2 sum1, vec4 z) {
    star_lens_loop(sum.x, sum1.x, z.xz);
}

void disc_lens_loop(inout float sum, inout float sum1, vec2 z) {
    sum1 = sum;
    sum += 1.0/(1.0 + pow(cmod(z - q2)/q1.x, exp(q3.x - 1.0)));
}
void disc_lens_loop(inout vec2 sum, inout vec2 sum1, vec4 z) {
    disc_lens_loop(sum.x, sum1.x, z.xz);
}

void sine_lens_loop(inout float sum, inout float sum1, vec2 z) {
    sum1 = sum;
    // sum += 1.0/(1.0 + pow(sqrt(abs(q1.x*z.x*z.x + q1.y*z.y*z.y)), exp(q2.x - 1.0)));
    // sum += 1.0/(1.0 + pow(cmod(csin(rotate(q1.x*z, q2.x) + q3)), exp(q4.x - 1.0)));
    sum += 1.0/(1.0 + pow(cmod(csin(rotate(z/q1.x, q2.x) - q3)), exp(q4.x - 1.0)));
}
void sine_lens_loop(inout vec2 sum, inout vec2 sum1, vec4 z) {
    sine_lens_loop(sum.x, sum1.x, z.xz);
}

float field_lines_final(uint n, vec2 z, vec2 z1, vec2 alpha, bool textureIn) {

    // float m = escape_smooth_final(n, z, z1, textureIn);
     float w = 0.5*(carg(cdiv(z, alpha))/pi + 1.0);
     float u = mod(w + q3.x/q1.x, 1.0/q1.x) - 0.5/q1.x;

    // if (abs(u) < 0.5*q2.x/q1.x) return m; else return 0.0;
    // if (abs(u) < 0.5*q2.x/q1.x) return abs(u); else return 0.0;
    // vec4 color = texture(image, vec2(u*q1.x/q2.x + 0.5, mod(75.0*m, 1.0)));
    // if (abs(u) < 0.5*q2.x/q1.x) return uintBitsToFloat(packFloatsToUint(color)); else return 0.0;

     if (abs(u) < 0.5*q2.x/q1.x) return abs(u); else return 0.0;

}
float field_lines_final(uint n, vec4 z, vec4 z1, vec2 alpha, bool textureIn) {
    return field_lines_final(n, z.xz, z1.xz, alpha, textureIn);
}

float field_lines2_final(vec2 z, vec2 z1, bool textureIn) {
//    float modz1;
//    float div = divergence(z, z1, modz1, textureIn);
//    float t = -log(log(modz1)/log(R))/log(div);
//    return (1.0 - t)*carg(z1) + t*carg(z);
    if (z.x > 0.0) return 1.0; else return 0.0;
}

void orbit_trap_image_over_loop(vec2 z, inout vec4 color, float imageRatio) {
    vec2 index = 0.5*(rotate(z - q2, q3.x)/(q1.x*vec2(1.0, imageRatio)) + 1.0);
    if (all(inRange(index, 0.0, 1.0))) {
        if (color.a < 1.0) {
            vec4 p = texture(image, index);
            color += p*(1.0 - color.a);
        }
    }
}
void orbit_trap_image_over_loop(vec4 z, inout vec4 color, float imageRatio) {
    orbit_trap_image_over_loop(z.xz, color, imageRatio);
}
uint orbit_trap_image_over_final(vec4 color) {
    uvec4 p = uvec4(color*254.0);
    return (p.r << 24) + (p.g << 16) + (p.b << 8) + p.a;
}

void orbit_trap_image_under_loop(vec2 z, inout vec4 color, float imageRatio) {
    vec2 index = 0.5*(rotate(z - q2, q3.x)/(q1.x*vec2(1.0, imageRatio)) + 1.0);
    if (all(inRange(index, 0.0, 1.0))) {
        vec4 p = texture(image, index);
        color = p.a*p + (1.0 - p.a)*color;
    }
}
void orbit_trap_image_under_loop(vec4 z, inout vec4 color, float imageRatio) {
    orbit_trap_image_under_loop(z.xz, color, imageRatio);
}
uint orbit_trap_image_under_final(vec4 color) {
    return orbit_trap_image_over_final(color);
}

float escape_smooth_dist_estim_final(uint n, vec2 z, vec2 z1, float modsqrz, vec2 alpha, bool textureIn) {
    float dist = dist_estim_abs_final(modsqrz, alpha);
    if (dist == 0.0) return specialValue;
    else return escape_smooth_final(n, z, z1, textureIn);
}
float escape_smooth_dist_estim_final(uint n, vec4 z, vec4 z1, vec2 modsqrz, vec2 alpha, bool textureIn) {
    return escape_smooth_dist_estim_final(n, z.xz, z1.xz, modsqrz.x, alpha, textureIn);
}

float highlight_iter_final(uint n) {
    if (n == uint(q1.x)) return specialValue;
    else return 0.0;
}

float kleinian_dist_final(vec2 z, vec2 alpha, vec2 t) {

    float s;
    if (abs(z.y - t.x) < abs(z.y)) s = z.y/t.x;
    else s = abs(z.y - t.x)/t.x;
    float d = s*log(s)/cmod(alpha);

    // return d;
    if (abs(d)*200.0/xScale.x < log(q1.x + 1.0)) return specialValue; else return 0.0;

}
float kleinian_dist_final(vec4 z, vec2 alpha, vec2 t) {
    return kleinian_dist_final(z.xz, alpha, t);
}

float kleinian_ab_final(vec2 z, vec2 t) {
    if (abs(z.y - t.x) < abs(z.y)) return 2.0; else return 0.0;
}
float kleinian_ab_final(vec4 z, vec2 t) {
    return kleinian_ab_final(z.xz, t);
}

void kleinian_switch_loop(vec2 z, bool underPrev, inout uint switchCount, vec2 t, float K, float M, float k) {
    bool under = z.y < 0.5*t.x + sign(t.y)*sign(z.x + 0.5*t.y)*K*t.x*(1.0 - exp(-M*abs(z.x + 0.5*t.y)));
    if (under != underPrev) switchCount++;
    underPrev = under;
}

float kleinian_switch_final(uint switchCount) {
    return float(switchCount);
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
    if (eps < R) { return true; }
    return false;
}
bool converge(inout float eps, vec4 z, vec4 z1) {
    eps = cmodsqr(cadd(z, -z1)).x;
    if (eps < R) { return true; }
    return false;
}

bool escape_tri(vec2 z) {
    return z.y > sqrt(3.0)*z.x + R || z.y > -sqrt(3.0)*z.x + R || z.y < -R;
}
bool escape_tri(vec4 z) {
    return z.z > sqrt(3.0)*z.x + R || z.z > -sqrt(3.0)*z.x + R || z.z < -R;
}

bool escape_sqr(vec2 z) {
    return abs(z.x) > 0.5*R || abs(z.y) > 0.5*R;
}
bool escape_sqr(vec4 z) {
    return abs(z.x) > 0.5*R || abs(z.z) > 0.5*R;
}

bool escape_pent(vec2 z) {
    return  -z.y > 0.726542528*z.x + R ||
            -z.y > -0.726542528*z.x + R ||
            -z.y < 3.077683537*(z.x - R*0.9510565163) + R*0.309016994375 ||
            -z.y < -3.077683537*(z.x - R*-0.5877852522925) + R*-0.8090169943749475 ||
            -z.y < -R*0.809016994375;
}
bool escape_pent(vec4 z) {
    return  -z.z > 0.726542528*z.x + R ||
            -z.z > -0.726542528*z.x + R ||
            -z.z < 3.077683537*(z.x - R*0.9510565163) + R*0.309016994375 ||
            -z.z < -3.077683537*(z.x - R*-0.5877852522925) + R*-0.8090169943749475 ||
            -z.z < -R*0.809016994375;
}




void main() {

    bool textureIn = false;
    float textureValue = 0.0;
    uint textureValueInt = 0u;
    uint textureType = 0u;

    float eps = 0.0;
    vec2 alpha = alpha0;
    vec2 beta = vec2(0.0);
    float il = 1.0/log(power);
    float llr = log(log(R)/power);
    float useUniforms = p1.x + p2.x + p3.x + p4.x + q1.x + q2.x + q3.x + q4.x + x0 + y0 + R;

    // generalInit
    // seedInit
    // shapeInit
    // textureInit

    for (uint n = 0u; n <= maxIter; n++) {

        if (n == maxIter) {
            textureIn = true;
            textureType = 1u;
            // textureFinal
            break;
        }

        z2 = z1;
        z1 = z;

        // shapeLoop
        // textureLoop

        /* conditional */ {
            // textureFinal
            break;
        }

    }

    textureValueInt = (textureValueInt >> 1) << 1;
    fragmentColor = textureValueInt + textureType;

}

#version 320 es
#define SPLIT 8193.

precision highp float;
uniform int maxIter;
uniform vec2 lightPos;
uniform vec2 xScale;
uniform vec2 yScale;
uniform vec2 xOffset;
uniform vec2 yOffset;
uniform vec2 screenRes;
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

vec2 df64_add(vec2 a, vec2 b) {
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

vec2 df64_mult(vec2 a, vec2 b) {
    vec2 p;
    p = twoProd(a.x, b.x);
    p.y += a.x * b.y;
    p.y += a.y * b.x;
    p = quickTwoSum(p.x, p.y);
    return p;
}

vec2 twoSqr(float a) {
    float p = a*a;
    vec2 s = split(a);
    float e = ((s.x*s.x - p) + 2.0*s.x*s.y) + s.y*s.y;
    return vec2(p, e);
}

vec2 df64_sqr(vec2 a) {
    vec2 p;
    p = twoSqr(a.x);
    p.y += 2.0*a.x*a.y;
    p = quickTwoSum(p.x, p.y);
    return p;
}

bool df64_gt(vec2 a, vec2 b) {
    return (a.x > b.x || (a.x == b.x && a.y > b.y));
}

vec2 df64_mandelbrot_x(vec2 X, vec2 Y, vec2 xC) {
    // return df64_add(df64_mult(df64_add(X, Y), df64_add(X, -Y)), xC);
    return df64_add(df64_add(df64_sqr(X), -df64_sqr(Y)), xC);
}

vec2 df64_mandelbrot_y(vec2 X, vec2 Y, vec2 yC) {
    return df64_add(df64_mult(vec2(2.0, 0.0), df64_mult(X, Y)), yC);
}

vec2 df64_mod2(vec2 X, vec2 Y) {
    // return df64_add(df64_mult(X, X), df64_mult(Y, Y));
    return df64_add(df64_sqr(X), df64_sqr(Y));
}




vec2 mult(vec2 w1, vec2 w2) {
    return vec2(w1.x*w2.x - w1.y*w2.y, w1.x*w2.y + w2.x*w1.y);
}

vec2 conj(vec2 w) {
    return vec2(w.x, -w.y);
}

float modulus(vec2 w) {
    return sqrt(w.x*w.x + w.y*w.y);
}

vec2 div(vec2 w1, vec2 w2) {
    vec2 u = mult(w1, conj(w2));
    return u/dot(w2, w2);
}

float atan2(vec2 w) {
    float pi = 3.141593;
    if (w.x > 0.0) {
        return atan(w.y, w.x);
    }
    else if (w.x < 0.0) {
        if (w.y >= 0.0) {
            return atan(w.y, w.x) + pi;
        }
        else {
            return atan(w.y, w.x) - pi;
        }
    }
    else {
        if (w.y > 0.0) {
            return pi/2.0;
        }
        else if (w.y < 0.0) {
            return -1.0*pi/2.0;
        }
        else {
            return 0.0;
        }
    }
}





void main() {

    vec2 screenPos = 2.0*(gl_FragCoord.xy / screenRes) - vec2(1.0, 1.0);
    vec2 xC = df64_add(df64_mult(xScale, vec2(screenPos.x, 0.0)), xOffset);
    vec2 yC = df64_add(df64_mult(yScale, vec2(screenPos.y, 0.0)), yOffset);

    float num_colors = 4.0;
    float cmap_cycles = 2.0;
    vec2 MOD2 = vec2(0.0);
    //float MOD2 = 0.0;

    float height = 1.25;

    vec2 X = vec2(0.0);
    vec2 Y = vec2(0.0);
    vec2 Y_temp = vec2(0.0);
    // vec2 Z = vec2(0.0, 0.0);
    // vec2 a = vec2(0.0, 0.0);
    // vec2 b = vec2(0.0, 0.0);
    // vec2 u = vec2(0.0, 0.0);
    float pi = 3.141593;

    vec3 color    =  vec3(0.0, 0.0, 0.0);
    vec3 black    =  vec3(0.0, 0.0, 0.0);
    vec3 purple   =  vec3(0.3, 0.0, 0.5);
    vec3 red      =  vec3(1.0, 0.0, 0.0);
    vec3 pink     =  vec3(1.0, 0.3, 0.4);
    vec3 yellow   =  vec3(1.0, 1.0, 0.0);
    vec3 white    =  vec3(1.0, 1.0, 1.0);
    vec3 darkblue =  vec3(0.0, 0.15, 0.25);
    vec3 orange   =  vec3(1.0, 0.6, 0.0);


    // darkblue, white, orange, purple

    vec3 c1 = darkblue;
    vec3 c2 = white;
    vec3 c3 = vec3(0.8, 0.2, 0.2);
    vec3 c4 = purple;

    for (int i = 0; i < maxIter; i++) {
        if (i == maxIter - 1) {
            color = black;
            break;
        }

        // iterate second derivative
        // b = 2.0*(mult(b, Z) + mult(a, a));

        // iterate derivative
        // a = 2.0*mult(a, Z);
        // a.x = a.x + 1.0;

        // iterate z
        Y_temp = df64_mandelbrot_y(X, Y, yC);
        X = df64_mandelbrot_x(X, Y, xC);
        Y = Y_temp;

        // MOD2 = dot(Z, Z);
        MOD2 = df64_mod2(X, Y);
        if (MOD2.x > 4.0) {


//             float lo = 0.5*log(MOD2);
//             u = mult(  mult(Z, a), (1.0 + lo)*conj(mult(a, a)) - lo*conj(mult(Z, b))  );
//             u = div(Z, a);
//             u = u/modulus(u);

//             float t = u.x*lightPos.x + u.y*lightPos.y + height;
//             t = t/(1.0 + height);
//             if (t < 0.0) {
//                 t = 0.0;
//             }
//             color = t*white;

            float m = cmap_cycles*num_colors*(float(i)-log(0.5*log(MOD2.x))/log(2.0))/float(maxIter);
            float n = m - (num_colors * floor(m/num_colors));
            if      (n >= 0.0 && n < 1.0) {  color = (1.0-n) * c1   +   (n)     * c2;  }
            else if (n >= 1.0 && n < 2.0) {  color = (2.0-n) * c2   +   (n-1.0) * c3;  }
            else if (n >= 2.0 && n < 3.0) {  color = (3.0-n) * c3   +   (n-2.0) * c4;  }
            else if (n >= 3.0 && n < 4.0) {  color = (4.0-n) * c4   +   (n-3.0) * c1;  }


//            float n = float(i)/float(maxIter);
//            color = vec3(1.0-n, 1.0-n, 1.0-n);


            break;
        }

    }

    fragmentColor.rgb = color;
    fragmentColor.a = 1.0;

}
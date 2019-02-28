#version 320 es
#define SPLIT 8193.
#define R 1000.

precision highp float;
uniform int maxIter;
uniform vec2 xTouchPos;
uniform vec2 yTouchPos;
uniform vec2 xScale;
uniform vec2 yScale;
uniform vec2 xOffset;
uniform vec2 yOffset;
uniform vec2 texRes;

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

vec4 df64_complex_mult(vec2 X, vec2 Y, vec2 A, vec2 B) {

    vec2 k1 = df64_mult(A, df64_add(X, Y));
    vec2 k2 = df64_mult(X, df64_add(B, -A));
    vec2 k3 = df64_mult(Y, df64_add(A, B));
    vec2 U = df64_add(k1, -k3);
    vec2 V = df64_add(k1, k2);
    return vec4(U, V);

}




void main() {

//    vec2 screenPos = 2.0*(gl_FragCoord.xy / texRes) - vec2(1.0, 1.0);

    // use mandelbrot C components
    vec2 xC = df64_add(df64_mult(xScale, vec2(viewPos.x, 0.0)), xOffset);
    vec2 yC = df64_add(df64_mult(yScale, vec2(viewPos.y, 0.0)), yOffset);

    // use julia C components
//    vec2 xC = xTouchPos;
//    vec2 yC = yTouchPos;

    float num_colors = 5.0;
    float cmap_cycles = 3.0;
    vec2 MOD2 = vec2(0.0);

    float height = 1.25;

    // use julia Z components
//    vec2 X = df64_add(df64_mult(xScale, vec2(viewPos.x, 0.0)), xOffset);
//    vec2 Y = df64_add(df64_mult(yScale, vec2(viewPos.y, 0.0)), yOffset);

    // use mandelbrot Z components
    vec2 X = vec2(0.0);
    vec2 Y = vec2(0.0);

    vec2 Y_temp;

//    vec2 X1 = vec2(0.0);
//    vec2 Y1 = vec2(0.0);
//
//    vec2 X2 = vec2(0.0);
//    vec2 Y2 = vec2(0.0);
//
//    vec2 X3 = vec2(0.0);
//    vec2 Y3 = vec2(0.0);
//
//    vec2 X4 = vec2(0.0);
//    vec2 Y4 = vec2(0.0);
//
//    vec2 X5 = vec2(0.0);
//    vec2 Y5 = vec2(0.0);
//
//    vec2 X6 = vec2(0.0);
//    vec2 Y6 = vec2(0.0);
//
//    vec2 X7 = vec2(0.0);
//    vec2 Y7 = vec2(0.0);
//
//    vec2 X8 = vec2(0.0);
//    vec2 Y8 = vec2(0.0);
//
//    float eps = 0.0001;
//    bool repeat = false;
//    int period = 0;

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

    vec3 c1 = vec3(0.0, 0.1, 0.2);
    vec3 c2 = darkblue;
    vec3 c3 = white;
    vec3 c4 = vec3(0.9, 0.4, 0.2);
    vec3 c5 = purple * 0.5;
    vec3 c6 = black;

    for (int i = 0; i < maxIter; i++) {

        // iterate second derivative
        // b = 2.0*(mult(b, Z) + mult(a, a));

        // iterate derivative
        // a = 2.0*mult(a, Z);
        // a.x = a.x + 1.0;

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

        // iterate z
        Y_temp = df64_mandelbrot_y(X, Y, yC);
        X = df64_mandelbrot_x(X, Y, xC);
        Y = Y_temp;

//        if (i > 8) {
            // check for periodicity
//            if (abs(X.x - X1.x) < eps && abs(X.y - X1.y) < eps && abs(Y.x - Y1.x) < eps && abs(Y.y - Y1.y) < eps) {
//                period = 1;
//                repeat = true;
//            }
//            if (abs(X.x - X2.x) < eps && abs(X.y - X2.y) < eps && abs(Y.x - Y2.x) < eps && abs(Y.y - Y2.y) < eps) {
//                period = 2;
//                repeat = true;
//            }
//            if (abs(X.x - X3.x) < eps && abs(X.y - X3.y) < eps && abs(Y.x - Y3.x) < eps && abs(Y.y - Y3.y) < eps) {
//                period = 3;
//                repeat = true;
//            }
//            if (abs(X.x - X4.x) < eps && abs(X.y - X4.y) < eps && abs(Y.x - Y4.x) < eps && abs(Y.y - Y4.y) < eps) {
//                period = 4;
//                repeat = true;
//            }
//            if (abs(X.x - X5.x) < eps && abs(X.y - X5.y) < eps && abs(Y.x - Y5.x) < eps && abs(Y.y - Y5.y) < eps) {
//                period = 5;
//                repeat = true;
//            }
//            if (abs(X.x - X6.x) < eps && abs(X.y - X6.y) < eps && abs(Y.x - Y6.x) < eps && abs(Y.y - Y6.y) < eps) {
//                period = 6;
//                repeat = true;
//            }
//            if (abs(X.x - X7.x) < eps && abs(X.y - X7.y) < eps && abs(Y.x - Y7.x) < eps && abs(Y.y - Y7.y) < eps) {
//                period = 7;
//                repeat = true;
//            }
//            if (abs(X.x - X8.x) < eps && abs(X.y - X8.y) < eps && abs(Y.x - Y8.x) < eps && abs(Y.y - Y8.y) < eps) {
//                period = 8;
//                repeat = true;
//            }
//        }
        
//        if (repeat) {
//            color = vec3(0.0);
//            break;
//        }

        // check for escape
        MOD2 = df64_mod2(X, Y);
        if (MOD2.x > R) {

            // lighting calculation
            // float lo = 0.5*log(MOD2);
            // u = mult(  mult(Z, a), (1.0 + lo)*conj(mult(a, a)) - lo*conj(mult(Z, b))  );
            // u = div(Z, a);
            // u = u/modulus(u);

            // float t = u.x*lightPos.x + u.y*lightPos.y + height;
            // t = t/(1.0 + height);
            // if (t < 0.0) {
            //     t = 0.0;
            // }
            // color = t*white;


            // normalized values -- finite cycles
//             float m = cmap_cycles*num_colors*(float(i)-log(0.5*log(MOD2.x))/log(2.0))/float(maxIter);
//             float n = m - (num_colors * floor(m/num_colors));

            // unnormalized values -- infinite cycles
            float m = num_colors*(float(i)-log(0.5*log(MOD2.x))/log(2.0));
            float n = float(num_colors)/2.0*(cos(m/80.0) + 1.0);

            if      (n >= 0.0 && n < 1.0) {  color = (1.0-n) * c1   +   (n)     * c2;  }
            else if (n >= 1.0 && n < 2.0) {  color = (2.0-n) * c2   +   (n-1.0) * c3;  }
            else if (n >= 2.0 && n < 3.0) {  color = (3.0-n) * c3   +   (n-2.0) * c4;  }
            else if (n >= 3.0 && n < 4.0) {  color = (4.0-n) * c4   +   (n-3.0) * c5;  }
            else if (n >= 4.0 && n < 5.0) {  color = (5.0-n) * c5   +   (n-4.0) * c1;  }
            // else if (n >= 5.0 && n < 6.0) {  color = (6.0-n) * c6   +   (n-5.0) * c1;  }

//            color = vec3(0.0);

            // float a = 0.45;
            // float b = 0.8;
            // float c = 0.3;
            // color = vec3((1.0-cos(a*float(i)))/2.0, (1.0-cos(b*float(i)))/2.0, (1.0-cos(c*float(i)))/2.0);

            break;

        }

    }

    fragmentColor.rgb = color;
    fragmentColor.a = 1.0;

}
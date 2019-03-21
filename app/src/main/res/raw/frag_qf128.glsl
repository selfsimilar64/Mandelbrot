#version 320 es
#define SPLIT 8193.
#define R 10000.

precision highp float;
uniform int maxIter;
uniform vec2 xTouchPos;
uniform vec2 yTouchPos;
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

vec4 qf128_add(vec4 a, vec4 b) {
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

vec4 qf128_mult(vec4 a, vec4 b) {
    
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

vec4 qf128_sqr(vec4 a) {

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

bool qf128_gt(vec4 a, float b) {
    return a.x > b;
}

vec4 qf128_mod2(vec4 a, vec4 b) {
    return qf128_add(qf128_sqr(a), qf128_sqr(b));
}

vec4 qf128_mandelbrot_x(vec4 X, vec4 Y, vec4 xC) {
    return qf128_add(qf128_add(qf128_sqr(X), -qf128_sqr(Y)), xC);
}

vec4 qf128_mandelbrot_y(vec4 X, vec4 Y, vec4 yC) {
    vec4 T = qf128_mult(X, Y);
    return qf128_add(qf128_add(T, T), yC);
}









vec2 conj(vec2 p) {
    return vec2(p.x, -p.y);
}

float mod2(vec2 p) {
    return p.x*p.x + p.y*p.y;
}

vec2 mult(vec2 p, vec2 q) {
    return vec2(p.x*q.x - p.y*q.y, p.y*q.x + p.x*q.y);
}

vec2 div(vec2 p, vec2 q) {
    return mult(p, conj(q)) / mod2(q);
}

float modulus(vec2 p) {
    return sqrt(p.x*p.x + p.y*p.y);
}



void main() {

    // use mandelbrot C components
    vec4 xC = qf128_add(qf128_mult(xScale, vec4(viewPos.x, 0., 0., 0.)), xOffset);
    vec4 yC = qf128_add(qf128_mult(yScale, vec4(viewPos.y, 0., 0., 0.)), yOffset);

    // use julia C components
    //    vec2 xC = xTouchPos;
    //    vec2 yC = yTouchPos;

    // use mandelbrot Z components
    vec4 X = vec4(0.);
    vec4 Y = vec4(0.);
    vec4 Y_temp;

    // use julia Z components
    //    vec2 X = qf128_add(qf128_mult(xScale, vec2(viewPos.x, 0.0)), xOffset);
    //    vec2 Y = qf128_add(qf128_mult(yScale, vec2(viewPos.y, 0.0)), yOffset);



    float num_colors = 5.;
    float cmap_cycles = 3.;
    vec4 MOD2 = vec4(0.);
    vec2 lightPos = vec2(1.);
    float height = 1.25;



//    vec4 X1, X2, X3, X4, X5, X6, X7, X8 = vec4(0.);
//    vec4 Y1, Y2, Y3, Y4, Y5, Y6, Y7, Y8 = vec4(0.);

//    float XY1e, XY2e, XY3e, XY4e, XY5e, XY6e, XY7e, XY8e;

    float eps = 0.005 * xScale.x;
    bool repeat = false;
    int period = 0;



    // vec2 Z = vec2(0.0, 0.0);
    vec2 a = vec2(0.);
    vec2 b = vec2(0.);
    vec2 u = vec2(0.);
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
    vec3 c3 = vec3(0.7);
    vec3 c4 = vec3(0.9, 0.4, 0.2);
    vec3 c5 = purple * 0.5;
    vec3 c6 = vec3(0.8, 0.2, 0.2);

    for (int i = 0; i < maxIter; i++) {

        // iterate second derivative
        b = 2.0*(mult(b, vec2(X.x, Y.x)) + mult(a, a));

        // iterate derivative
        a = 2.0*mult(a, vec2(X.x, Y.x));
        a.x = a.x + 1.0;




        // cycle previous values
//        X8 = X7;
//        Y8 = Y7;
//        X7 = X6;
//        Y7 = Y6;
//        X6 = X5;
//        Y6 = Y5;
//        X5 = X4;
//        Y5 = Y4;
//        X4 = X3;
//        Y4 = Y3;
//        X3 = X2;
//        Y3 = Y2;
//        X2 = X1;
//        Y2 = Y1;
//        X1 = X;
//        Y1 = Y;




        // iterate z
        Y_temp = qf128_mandelbrot_y(X, Y, yC);
        X = qf128_mandelbrot_x(X, Y, xC);
        Y = Y_temp;



//        XY1e = abs(X.x - X1.x) + abs(X.y - X1.y) + abs(Y.x - Y1.x) + abs(Y.y - Y1.y);
//        XY2e = abs(X.x - X2.x) + abs(X.y - X2.y) + abs(Y.x - Y2.x) + abs(Y.y - Y2.y);
//        XY3e = abs(X.x - X3.x) + abs(X.y - X3.y) + abs(Y.x - Y3.x) + abs(Y.y - Y3.y);
//        XY4e = abs(X.x - X4.x) + abs(X.y - X4.y) + abs(Y.x - Y4.x) + abs(Y.y - Y4.y);
//        XY5e = abs(X.x - X5.x) + abs(X.y - X5.y) + abs(Y.x - Y5.x) + abs(Y.y - Y5.y);
//        XY6e = abs(X.x - X6.x) + abs(X.y - X6.y) + abs(Y.x - Y6.x) + abs(Y.y - Y6.y);
//        XY7e = abs(X.x - X7.x) + abs(X.y - X7.y) + abs(Y.x - Y7.x) + abs(Y.y - Y7.y);
//        XY8e = abs(X.x - X8.x) + abs(X.y - X8.y) + abs(Y.x - Y8.x) + abs(Y.y - Y8.y);

        // check for periodicity
//        if (i > 8) {
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

//        if (repeat) {
//            color = vec3(float(i)/float(maxIter));
//            color = vec3(1.0, 0.0, 1.0);
//            color = vec3(0.0);
//            break;
//        }




        // check for escape
        MOD2 = qf128_mod2(X, Y);
        if (qf128_gt(MOD2, R)) {

            // normal calculation
            float lo = 0.5*log(MOD2.x);
            vec2 Zf = vec2(X.x, Y.x);
            u = mult(  mult(Zf, a), (1.0 + lo)*conj(mult(a, a)) - lo*conj(mult(Zf, b))  );
            u = div(Zf, a);
            u = u/modulus(u);

            // calculate rays for lighting calculations
            vec3 normRay = vec3(u.x, u.y, 1.0);
            normRay = normRay / length(normRay);
            vec3 lightRay = vec3(lightPos.x, lightPos.y, height);
            lightRay = lightRay / length(lightRay);
            vec3 viewRay = vec3(0.0, 0.0, 1.0);
            vec3 reflectRay = 2.0*dot(normRay, lightRay)*normRay - lightRay;

            // calculate lighting components
            float diffuse = dot(normRay, lightRay);
            diffuse = diffuse/(1.0 + height);
            if (diffuse < 0.0) { diffuse = 0.0; }
            float specular = pow(dot(reflectRay, viewRay), 1.5);
            if (specular < 0.0) { specular = 0.0; }


            // normalized values -- finite cycles
            //             float m = cmap_cycles*num_colors*(float(i)-log(0.5*log(MOD2.x))/log(2.0))/float(maxIter);
            //             float n = m - (num_colors * floor(m/num_colors));

            // unnormalized values -- infinite cycles
            float m = float(i)-log(0.5*log(MOD2.x))/log(2.0);
            //            float n = float(num_colors)/2.0*(cos(m/14.0) + 1.0);
            float n = float(num_colors)/2.0*(cos(2.0*pow(m + 5.0, 0.4) -  0.3) + 1.0);

            if      (n >= 0.0 && n < 1.0) {  color = (1.0-n) * c1   +   (n)     * c2;  }
            else if (n >= 1.0 && n < 2.0) {  color = (2.0-n) * c2   +   (n-1.0) * c3;  }
            else if (n >= 2.0 && n < 3.0) {  color = (3.0-n) * c3   +   (n-2.0) * c4;  }
            else if (n >= 3.0 && n < 4.0) {  color = (4.0-n) * c4   +   (n-3.0) * c5;  }
            else if (n >= 4.0 && n < 5.0) {  color = (5.0-n) * c5   +   (n-4.0) * c1;  }
            //            else if (n >= 5.0 && n < 6.0) {  color = (6.0-n) * c6   +   (n-5.0) * c1;  }

            //            color = vec3(0.0);

            //            color = 2.5*(diffuse + 0.2)*color;
            color = 1.75*(diffuse + 0.2)*color + 0.75*vec3(specular + 0.01);
            //            color = vec3(specular);

            // float a = 0.45;
            // float b = 0.8;
            // float c = 0.3;
            // color = vec3((1.0-cos(a*float(i)))/2.0, (1.0-cos(b*float(i)))/2.0, (1.0-cos(c*float(i)))/2.0);

            break;

        }

    }

    fragmentColor = vec4(color, 1.0);
//    fragmentColor = vec4(1.);

}
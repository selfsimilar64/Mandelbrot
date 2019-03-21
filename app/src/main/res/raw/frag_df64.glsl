#version 320 es
#define SPLIT 8193.
#define R 10000.

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

vec2 mult(vec2 a, vec2 b) {
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

vec2 sqr(vec2 a) {
    vec2 p;
    p = twoSqr(a.x);
    p.y += 2.0*a.x*a.y;
    p = quickTwoSum(p.x, p.y);
    return p;
}

bool gt(vec2 a, vec2 b) {
    return (a.x > b.x || (a.x == b.x && a.y > b.y));
}

vec2 mandelbrot_x(vec2 X, vec2 Y, vec2 xC) {
    return add(add(sqr(X), -sqr(Y)), xC);
}

vec2 mandelbrot_y(vec2 X, vec2 Y, vec2 yC) {
    vec2 T = mult(X, Y);
    return add(add(T, T), yC);
}

vec2 perp_mandelbrot_y(vec2 X, vec2 Y, vec2 yC) {
    vec2 T = mult(vec2(abs(X.x), abs(X.y)), Y);
    return add(-add(T, T), yC);
}

vec2 modSqrDF(vec2 X, vec2 Y) {
    return add(sqr(X), sqr(Y));
}

vec4 cMultDF(vec2 X, vec2 Y, vec2 A, vec2 B) {

    vec2 k1 = mult(A, add(X, Y));
    vec2 k2 = mult(X, add(B, -A));
    vec2 k3 = mult(Y, add(A, B));
    vec2 U = add(k1, -k3);
    vec2 V = add(k1, k2);
    return vec4(U, V);

}



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



void main() {

//    vec2 screenPos = 2.0*(gl_FragCoord.xy / texRes) - vec2(1.0, 1.0);

    // use mandelbrot C components
    vec2 xC = add(mult(xScale, vec2(viewPos.x, 0.0)), xOffset);
    vec2 yC = add(mult(yScale, vec2(viewPos.y, 0.0)), yOffset);

    // use julia C components
//    vec2 xC = xTouchPos;
//    vec2 yC = yTouchPos;

    // use mandelbrot Z components
    vec2 X = vec2(0.0);
    vec2 Y = vec2(0.0);
    vec2 Y_temp;

    // use julia Z components
//    vec2 X = add(mult(xScale, vec2(viewPos.x, 0.0)), xOffset);
//    vec2 Y = add(mult(yScale, vec2(viewPos.y, 0.0)), yOffset);



    float num_colors = 5.0;
    float cmap_cycles = 3.0;
    vec2 MOD2 = vec2(0.0);
    vec2 lightPos = vec2(1.0);
    float height = 1.25;



    vec2 X1, X2, X3, X4, X5, X6, X7, X8 = vec2(0.0);
    vec2 Y1, Y2, Y3, Y4, Y5, Y6, Y7, Y8 = vec2(0.0);

    float XY1e, XY2e, XY3e, XY4e, XY5e, XY6e, XY7e, XY8e;

    float eps = 0.001 * xScale.x;
    bool repeat = false;
    int period = 0;



    // vec2 Z = vec2(0.0, 0.0);
    vec2 a = vec2(0.0, 0.0);
    vec2 b = vec2(0.0, 0.0);
    vec2 u = vec2(0.0, 0.0);
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
        b = 2.0*(cMultSF(b, vec2(X.x, Y.x)) + cMultSF(a, a));

        // iterate derivative
        a = 2.0*cMultSF(a, vec2(X.x, Y.x));
        a.x = a.x + 1.0;




        // cycle previous values
        X8 = X7;
        Y8 = Y7;

        X7 = X6;
        Y7 = Y6;

        X6 = X5;
        Y6 = Y5;

        X5 = X4;
        Y5 = Y4;

        X4 = X3;
        Y4 = Y3;

        X3 = X2;
        Y3 = Y2;

        X2 = X1;
        Y2 = Y1;

        X1 = X;
        Y1 = Y;




        // iterate z
        Y_temp = mandelbrot_y(X, Y, yC);
        X = mandelbrot_x(X, Y, xC);
        Y = Y_temp;



        XY1e = abs(X.x - X1.x) + abs(X.y - X1.y) + abs(Y.x - Y1.x) + abs(Y.y - Y1.y);
        XY2e = abs(X.x - X2.x) + abs(X.y - X2.y) + abs(Y.x - Y2.x) + abs(Y.y - Y2.y);
        XY3e = abs(X.x - X3.x) + abs(X.y - X3.y) + abs(Y.x - Y3.x) + abs(Y.y - Y3.y);
        XY4e = abs(X.x - X4.x) + abs(X.y - X4.y) + abs(Y.x - Y4.x) + abs(Y.y - Y4.y);
        XY5e = abs(X.x - X5.x) + abs(X.y - X5.y) + abs(Y.x - Y5.x) + abs(Y.y - Y5.y);
        XY6e = abs(X.x - X6.x) + abs(X.y - X6.y) + abs(Y.x - Y6.x) + abs(Y.y - Y6.y);
        XY7e = abs(X.x - X7.x) + abs(X.y - X7.y) + abs(Y.x - Y7.x) + abs(Y.y - Y7.y);
        XY8e = abs(X.x - X8.x) + abs(X.y - X8.y) + abs(Y.x - Y8.x) + abs(Y.y - Y8.y);

        if (i > 8) {
        // check for periodicity
            if (XY1e < eps) {
                period = 1;
                repeat = true;
            }
            if (XY2e < eps) {
                period = 2;
                repeat = true;
            }
            if (XY3e < eps) {
                period = 3;
                repeat = true;
            }
            if (XY4e < eps) {
                period = 4;
                repeat = true;
            }
            if (XY5e < eps) {
                period = 5;
                repeat = true;
            }
            if (XY6e < eps) {
                period = 6;
                repeat = true;
            }
            if (XY7e < eps) {
                period = 7;
                repeat = true;
            }
            if (XY8e < eps) {
                period = 8;
                repeat = true;
            }
        }

        if (repeat) {
            // color = vec3(float(i)/float(maxIter));
            // color = vec3(1.0, 0.0, 1.0);
            color = vec3(0.0);
            break;
        }




        // check for escape
        MOD2 = modSqrDF(X, Y);
        if (gt(MOD2, vec2(R, 0.0))) {

            // normal calculation
            float lo = 0.5*log(MOD2.x);
            vec2 Zf = vec2(X.x, Y.x);
            u = cMultSF(  cMultSF(Zf, a), (1.0 + lo)*conj(cMultSF(a, a)) - lo*conj(cMultSF(Zf, b))  );
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
    gl_FragDepth = 0.0;

}
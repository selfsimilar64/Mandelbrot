#version 320 es

precision highp float;
uniform int maxIter;
uniform vec2 lightPos;
uniform vec2 xScale;
uniform vec2 yScale;
uniform vec2 xOffset;
uniform vec2 yOffset;
uniform vec2 screenRes;
out vec4 fragmentColor;


vec2 ds_add (vec2 dsa, vec2 dsb) {
    vec2 dsc;
    float t1, t2, e;

    t1 = dsa.x + dsb.x;
    e = t1 - dsa.x;
    t2 = ((dsb.x - e) + (dsa.x - (t1 - e))) + dsa.y + dsb.y;

    dsc.x = t1 + t2;
    dsc.y = t2 - (dsc.x - t1);
    return dsc;
}

vec2 ds_sub (vec2 dsa, vec2 dsb) {
    vec2 dsc;
    float e, t1, t2;

    t1 = dsa.x - dsb.x;
    e = t1 - dsa.x;
    t2 = ((-dsb.x - e) + (dsa.x - (t1 - e))) + dsa.y - dsb.y;

    dsc.x = t1 + t2;
    dsc.y = t2 - (dsc.x - t1);
    return dsc;
}

vec2 ds_mul (vec2 dsa, vec2 dsb) {
    vec2 dsc;
    float c11, c21, c2, e, t1, t2;
    float a1, a2, b1, b2, cona, conb, split = 8193.;

    cona = dsa.x * split;
    conb = dsb.x * split;
    a1 = cona - (cona - dsa.x);
    b1 = conb - (conb - dsb.x);
    a2 = dsa.x - a1;
    b2 = dsb.x - b1;

    c11 = dsa.x * dsb.x;
    c21 = a2 * b2 + (a2 * b1 + (a1 * b2 + (a1 * b1 - c11)));

    c2 = dsa.x * dsb.y + dsa.y * dsb.x;

    t1 = c11 + c2;
    e = t1 - c11;
    t2 = dsa.y * dsb.y + ((c2 - e) + (c11 - (t1 - e))) + c21;

    dsc.x = t1 + t2;
    dsc.y = t2 - (dsc.x - t1);

    return dsc;
}

float ds_compare(vec2 dsa, vec2 dsb) {
    if (dsa.x < dsb.x) return -1.;
    else if (dsa.x == dsb.x) {
        if (dsa.y < dsb.y) return -1.;
        else if (dsa.y == dsb.y) return 0.;
        else return 1.;
        }
    else return 1.;
}


vec4 ds_mandelbrot(vec4 Z, vec2 xC, vec2 yC) {
    vec2 X = Z.xy;
    vec2 Y = Z.zw;
    vec2 U = ds_add(ds_mul(ds_add(X, Y), ds_sub(X, Y)), xC);
    vec2 V = ds_add(ds_mul(vec2(2.0, 0.0), ds_mul(X, Y)), yC);
    return vec4(U.x, U.y, V.x, V.y);
}

vec2 ds_mod2(vec4 Z) {
    vec2 X = Z.xy;
    vec2 Y = Z.zw;
    return ds_add(ds_mul(X, X), ds_mul(Y, Y));
}

// vec2 mandelbrot(vec2 z) {
//     return vec2(z.x*z.x - z.y*z.y, 2.0*z.x*z.y) + C;
// }

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
    vec2 xC = ds_add(ds_mul(xScale, vec2(screenPos.x, 0.0)), xOffset);
    vec2 yC = ds_add(ds_mul(yScale, vec2(screenPos.y, 0.0)), yOffset);

    float num_colors = 4.0;
    float cmap_cycles = 2.0;
    vec2 MOD2 = vec2(0.0);
    //float MOD2 = 0.0;

    float height = 1.25;

    vec4 Z = vec4(0.0);
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

    vec3 c1 = darkblue;
    vec3 c2 = white;
    vec3 c3 = orange;
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
        Z = ds_mandelbrot(Z, xC, yC);

        // MOD2 = dot(Z, Z);
        MOD2 = ds_mod2(Z);
        if (ds_compare(MOD2, vec2(4.0, 0.0)) == 1.0) {


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
#version 320 es
#define R 1000.
#define pi 3.141592654
#define Sn 1e-18
#define Sp 1e18
#define Sh 1e-9

precision highp float;
uniform int maxIter;
uniform float xTouchPos;
uniform float yTouchPos;
uniform float xScale;
uniform float yScale;
uniform float xOffset;
uniform float yOffset;

in vec4 viewPos;
out vec4 fragmentColor;



float _m(float _a, float _b) {
    return _a*_b * Sp;
}


vec2 mandelbrot(vec2 z, vec2 C) {
    return vec2(z.x*z.x - z.y*z.y, 2.0*z.x*z.y) + C;
}

vec2 exponential(vec2 z, vec2 C) {
    float t = exp(z.x);
    float s = sin(z.y);
    return vec2(t*(s + pi), t*s) + C;
}

vec2 mult(vec2 w1, vec2 w2) {
    return vec2(w1.x*w2.x - w1.y*w2.y, w1.x*w2.y + w2.x*w1.y);
}

vec2 _mult(vec2 _a, vec2 _b) {
    return vec2(_m(_a.x, _b.x) - _m(_a.y, _b.y), _m(_a.x, _b.y) + _m(_b.x, _a.y));
}

vec2 _sqr(vec2 _a) {
    return vec2(_m(_a.x, _a.x) - _m(_a.y, _a.y), 2.0*_m(_a.x, _a.y));
}

vec2 conj(vec2 w) {
    return vec2(w.x, -w.y);
}

float modulus(vec2 w) {
    return sqrt(w.x*w.x + w.y*w.y);
}

float _modulus(vec2 _a) {
    return sqrt(_m(_a.x, _a.x) + _m(_a.y, _a.y)) * Sh;
}

vec2 div(vec2 w1, vec2 w2) {
    vec2 u = mult(w1, conj(w2));
    return u/dot(w2, w2);
}

float atan2(vec2 w) {
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

vec2 sine(vec2 z) {
    float u = 0.5*(exp(z.y)+exp(-z.y))*sin(z.x);
    float v = 0.5*(exp(z.y)-exp(-z.y))*cos(z.x);
    return vec2(u, v);
}





void main() {

//    vec2 screenPos = 2.0*(gl_FragCoord.xy / texRes) - vec2(1.0, 1.0);    // range [-1, 1]

    float xC = xScale * viewPos.x + xOffset;
    float yC = yScale * viewPos.y + yOffset;

    vec2 C = vec2(xC, yC);

    float num_colors = 5.0;
    float cmap_cycles = 2.0;
    float MOD2 = 0.0;

    vec2 lightPos = vec2(1.0);
    float height = 1.25;

    vec2 Z = vec2(0.);
    // vec2 Z = C;
    vec2 _a = vec2(0.0, 0.0);
    vec2 _b = vec2(0.0, 0.0);
    vec2 _u = vec2(0.0, 0.0);
    vec2 v = vec2(0.0, 0.0);

    vec3 color    =  vec3(0.);
    vec3 tusk    =  vec3(237.0, 205.0, 185.0) / 256.0;
    vec3 black    =  vec3(0.0, 0.0, 0.0);
    vec3 purple   =  vec3(0.3, 0.0, 0.5);
    vec3 red      =  vec3(1.0, 0.0, 0.0);
    vec3 pink     =  vec3(1.0, 0.3, 0.4);
    vec3 yellow   =  vec3(1.0, 1.0, 0.0);
    vec3 white    =  vec3(1.0, 1.0, 1.0);
    vec3 darkblue =  vec3(0.0, 0.15, 0.25);
    vec3 orange   =  vec3(1.0, 0.6, 0.0);

//    vec3 c1 = white;
//    vec3 c2 = black;

    vec3 c1 = vec3(0.0, 0.1, 0.2);
    vec3 c2 = darkblue;
    vec3 c3 = vec3(0.7);
    vec3 c4 = vec3(0.9, 0.4, 0.2);
    vec3 c5 = purple * 0.5;
//    vec3 c6 = vec3(0.8, 0.2, 0.2);

    for (int i = 0; i < maxIter; i++) {
        if (i == maxIter - 1) {
            color = black;
            break;
        }

        // iterate second derivative
        _b = 2.0*(mult(_b, Z) + _sqr(_a));

        // iterate derivative
        _a = 2.0*mult(_a, Z);
        _a.x = _a.x + Sn;

        // iterate z
        // Z = sine(mult(Z, C))+C;
        Z = mandelbrot(Z, C);
        // Z = exponential(Z, C);
        // Z = sine(mult(Z, C));

        MOD2 = dot(Z, Z);
        if (MOD2 > R) {

            // normal calculation
            float lo = 0.5*log(MOD2);
            _u = _mult(  mult(Z, _a), (1.0 + lo)*conj(_sqr(_a)) - lo*conj(mult(Z, _b))  );
//            u = div(Z, a);
            v = _u/_modulus(_u);

            // calculate rays for lighting calculations
            vec3 normRay = vec3(v.x, v.y, 1.0);
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
//            float m = cmap_cycles*num_colors*float(i)/float(maxIter);
//             float m = cmap_cycles*num_colors*(float(i)-log(0.5*log(MOD2.x))/log(2.0))/float(maxIter);
//             float n = m - (num_colors * floor(m/num_colors));

            // unnormalized values -- infinite cycles
//            float n = num_colors*float(i)/float(maxIter);
            float m = float(i)-log(0.5*log(MOD2))/log(2.0);
//            float n = float(num_colors)/2.0*(cos(m/14.0) + 1.0);
            float n = float(num_colors)/2.0*(cos(2.0*pow(m + 5.0, 0.4) -  0.3) + 1.0);

            if      (n >= 0.0 && n < 1.0) {  color = (1.0-n) * c1   +   (n)     * c2;  }
            else if (n >= 1.0 && n < 2.0) {  color = (2.0-n) * c2   +   (n-1.0) * c3;  }
            else if (n >= 2.0 && n < 3.0) {  color = (3.0-n) * c3   +   (n-2.0) * c4;  }
            else if (n >= 3.0 && n < 4.0) {  color = (4.0-n) * c4   +   (n-3.0) * c5;  }
            else if (n >= 4.0 && n < 5.0) {  color = (5.0-n) * c5   +   (n-4.0) * c1;  }
//            else if (n >= 5.0 && n < 6.0) {  color = (6.0-n) * c6   +   (n-5.0) * c1;  }

//            color = vec3(0.0);
//            color = vec3(1.0 - float(i)/float(maxIter));

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

    // fragmentColor.rgb = vec3(gl_FragCoord.x/texRes.x, 0.0, gl_FragCoord.y/texRes.y);
    fragmentColor.rgb = color;
    fragmentColor.a = 1.0;
}
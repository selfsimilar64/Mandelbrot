#version 300 es
#define pi 3.141592654

precision highp float;
uniform sampler2D orbit;
uniform int orbitIter;
uniform int skipIter;
uniform int maxIter;
uniform vec2 A;
uniform vec2 B;
uniform vec2 C;
uniform float expShift;
uniform float R;
uniform float power;
uniform float x0;
uniform float y0;
uniform vec2 J;
uniform vec2 P1;
uniform vec2 P2;
uniform vec2 P3;
uniform vec2 P4;
uniform float Q1;
uniform float Q2;
uniform vec2 xScale;
uniform vec2 yScale;
uniform vec2 xCoord;
uniform vec2 yCoord;
uniform float sinRotate;
uniform float cosRotate;

in vec4 viewPos;
out vec4 fragmentColor;


vec2 cConjSF(vec2 w) {
    return vec2(w.x, -w.y);
}
vec2 cMultSF(vec2 w1, vec2 w2) {
    return vec2(w1.x*w2.x - w1.y*w2.y, w1.x*w2.y + w2.x*w1.y);
}
vec2 cSqrSF(vec2 w) {
    return vec2(w.x*w.x - w.y*w.y, 2.0*w.x*w.y);
}
vec2 cCubeSF(vec2 w) {
    return cMultSF(w, cSqrSF(w));
}
vec2 conjSF(vec2 w) {
    return vec2(w.x, -w.y);
}
float modSF(vec2 w) {
    return sqrt(w.x*w.x + w.y*w.y);
}
float modSqrSF(vec2 w) {
    return w.x*w.x + w.y*w.y;
}
vec2 cDivSF(vec2 w1, vec2 w2) {
    vec2 u = cMultSF(w1, conjSF(w2));
    return u/dot(w2, w2);
}
vec2 cSqrtSF(vec2 w) {
    float p = sqrt(modSF(w));
    float phi = atan(w.y, w.x) / 2.0;
    return p*vec2(cos(phi), sin(phi));
}
vec2 cExpSF(vec2 w) {
    return exp(w.x)*vec2(cos(w.y), sin(w.y));
}
vec2 cSinSF(vec2 z) {
    float u = 0.5*(exp(z.y)+exp(-z.y))*sin(z.x);
    float v = 0.5*(exp(z.y)-exp(-z.y))*cos(z.x);
    return vec2(u, v);
}
vec2 cCosSF(vec2 z) {
    float u = 0.5*(exp(-z.y) + exp(z.y))*cos(z.x);
    float v = 0.5*(exp(-z.y) - exp(z.y))*sin(z.x);
    return vec2(u, v);
}
vec2 cTanSF(vec2 z) {
    return cDivSF(cSinSF(z), cCosSF(z));
}
vec2 cLog(vec2 z) {
    float r = modSF(z);
    float theta = atan(z.y, z.x);
    return vec2(log(r + 0.01), theta);
}
vec2 cPow1(vec2 z, vec2 s) {
    float theta = atan(z.y, z.x);
    float r = sqrt(dot(z, z));
    float a = pow(r, s.x);
    float c = a*exp(-s.y*theta);
    float f = s.y*log(r) + s.x*theta;
    return c*vec2(cos(f), sin(f));
}
vec2 cPow2(float x, vec2 s) {

    float t;
    if (x == 0.0) { return vec2(0.0); }
    else if (x > 0.0) { t = 0.0; }
    else { t = pi; }

    float c = pow(x, s.x)*exp(-s.y*t);
    float f = s.x*t + s.y*log(x);
    return c*vec2(cos(f), sin(f));

}
vec2 cPow3(vec2 z, float p) {
    float theta = atan(z.y, z.x);
    float r = sqrt(dot(z, z));
    float c = pow(r, p);
    float f = p*theta;
    return c*vec2(cos(f), sin(f));
}
vec2 cPow4(vec2 z, vec2 s) {
    float theta = atan(z.y, z.x);
    float r = sqrt(dot(z, z));
    float a = pow(r, s.x);
    float b = exp(-s.y*theta);
    float c = a*b;
    float qq = s.y*log(r + 0.001);
    float f = qq + s.x*theta;
    return c*vec2(cos(f), sin(f));
}
vec2 boxFoldSF(vec2 z) {
    vec2 w = z;
    if (z.x < -1.0) { w.x = -2.0 - z.x; }
    else if (z.x > 1.0) { w.x = 2.0 - z.x; }
    if (z.y < -1.0) { w.y = -2.0 - z.y; }
    else if (z.y > 1.0) { w.y = 2.0 - z.y; }
    return w;
}
vec2 ballFoldSF(vec2 z) {
    float modZ = modSF(z);
    float coef = 1.0;
    if (modZ < 0.5) { coef = 4.0; }
    else if (modZ > 0.5 && modZ < 1.0) { coef = 1.0/(modZ*modZ); }
    return coef*z;
}


void main() {

    // GENERAL INIT
    vec4 colorParams = vec4(0.0);
    vec2 Z, Z1;
    vec2 D0 = vec2(xScale.x*viewPos.x, yScale.x*viewPos.y);
    D0 = vec2(D0.x*cosRotate - D0.y*sinRotate, D0.x*sinRotate + D0.y*cosRotate);
    vec2 D;
    vec2 D1 = cMultSF(A, D0) + cMultSF(B, cSqrSF(D0)) + cMultSF(C, cCubeSF(D0));
    float maxIterFloat = float(maxIter);
    vec2 W = vec2(0.0);
    float modW = 1.0;
    float modZ = 1.0;
    float useUniforms = P1.x + P2.x + P3.x + P4.x + Q1 + Q2 + x0 + y0 + R;


    // MAP INIT


    // TEXTURE INIT



    // MAIN LOOP
    for (int i = skipIter; i < orbitIter; i++) {

        // MAP LOOP
        Z  = texture(orbit, vec2( float(i+1) / maxIterFloat, 0.0) ).xy;
        Z1 = texture(orbit, vec2( float(i)   / maxIterFloat, 0.0) ).xy;

        D = 2.0*cMultSF(Z1, D1) + cSqrSF(D1) + D0;
        W = Z + D;

        modW = modSF(W);
        modZ = modSF(Z);

        if (modW > R) {

            float j = float(i)-log(0.5*log(modW))/log(2.0);
            colorParams.x = 10.0*j/float(maxIter);
            break;

        }

        if (i == orbitIter - 1) {

            float j = float(i)-log(0.5*log(modW))/log(2.0);
            colorParams.x = 10.0*j/float(maxIter);
            break;

        }

        if (modW/modZ < 0.001) {

            float j = float(i)-log(0.5*log(modW))/log(2.0);
            colorParams.x = 10.0*j/float(maxIter);
            colorParams.y = 1.0;
            break;

        }

        // GENERAL LOOP
        D1 = D;

    }


    fragmentColor = colorParams;


}

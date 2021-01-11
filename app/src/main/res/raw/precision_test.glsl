#version 300 es

precision highp float;
precision highp int;

out uint fragmentColor;

vec2 split(float a) {
    float _split = pow(2.0, 13.0) + 1.0;
    float _zero = pow(10.0, -30.0);
    float t = a*_split + _zero;
    float q = t - a + _zero;
    float a_hi = t - q;
    float a_lo = a - a_hi;
    return vec2(a_hi, a_lo);
}

void main() {

    float y = ( gl_FragCoord.y / 1560.0 ) * 26.0;     // [0, 26]
    float x = 1.0 - ( gl_FragCoord.x / 720.0 );      // [0, 1]

    float p = pow( 2.0, floor(y) );
    float b = p + x + 1e-45;
    float c = b - p;
    if (fract(y) >= 0.9) c = 0.0;

    fragmentColor = (floatBitsToUint(c)/2u)*2u;

}

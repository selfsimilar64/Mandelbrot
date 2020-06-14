#version 300 es

precision highp float;
out vec4 fragmentColor;

void main() {

    float y = ( gl_FragCoord.y / 1560.0 ) * 26.0;     // [0, 26]
    float x = 1.0 - ( gl_FragCoord.x / 720.0 );      // [0, 1]

    float p = pow( 2.0, floor(y) );
    float b = p + x + 1e-45;
    float c = b - p;
    if (fract(y) >= 0.9) {
        c = 0.0;
    }

    fragmentColor.x = b;
    fragmentColor.y = c;

}

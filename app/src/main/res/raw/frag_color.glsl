#version 300 es

precision highp float;
precision highp int;

uniform highp usampler2D tex;
uniform int numColors;
uniform vec3 palette[30];
uniform vec3 solidFillColor;
uniform float frequency;
uniform float phase;
uniform uint textureMode;
uniform float textureMin;
uniform float textureMax;

in vec2 texCoord;
out vec3 fragmentColor;


void main() {

    vec3 color = solidFillColor;
    // uvec2 s = texture(tex, texCoord).xy;
    uint r = texture(tex, texCoord).x;
    uint textureIn = r & 1u;
    // float s = unpackHalf2x16(r).x;
    float s = uintBitsToFloat(r);


    if (textureMode == 2u || textureMode == textureIn) {
        float i = (s - textureMin)/(textureMax - textureMin);
        float k = frequency*i + phase;
        float n = mod(float(numColors - 1)*k, float(numColors - 1));
        int p = int(floor(n));
        float q = mod(n, 1.0);
        color = (1.0 - q)*palette[p] + q*palette[p + 1];
    }

    fragmentColor = color;

}

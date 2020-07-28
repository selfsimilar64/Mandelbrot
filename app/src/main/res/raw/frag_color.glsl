#version 300 es

precision highp float;
precision highp int;

uniform highp usampler2D tex;
uniform int numColors;
uniform vec3 palette[25];
uniform vec3 solidFillColor;
uniform float frequency;
uniform float phase;
uniform float density;
uniform int textureMode;
uniform float textureMin;
uniform float textureMax;

in vec2 texCoord;
out vec3 fragmentColor;


void main() {

    vec3 color = solidFillColor;
    uvec2 s = texture(tex, texCoord).xy;

    if (textureMode == 2 || uint(textureMode) == s.y) {
        float i = (uintBitsToFloat(s.x) - textureMin)/(textureMax - textureMin);
        float j = frequency*i + phase;
        float n = mod(float(numColors - 1)*j, float(numColors - 1));
        int p = int(floor(n));
        float q = mod(n, 1.0);
        color = (1.0 - q)*palette[p] + q*palette[p + 1];
    }

    if (s.y == 3u) {
        color = vec3(0.85, 0.4, 0.4);
    }
    if (s.y == 4u) {
        color = vec3(0.4, 0.85, 0.4);
    }
    if (s.y == 5u) {
        color = vec3(0.4, 0.4, 0.85);
    }

    fragmentColor = color;

}

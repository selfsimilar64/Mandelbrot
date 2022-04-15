#version 300 es

precision highp float;
precision highp int;

uniform highp usampler2D tex;
uniform vec2 res;
uniform int lod;

in ivec2 texCoord;
out vec2 minmax;


void main() {

    float w1 = uintBitsToFloat((texelFetch(tex, 2*texCoord + ivec2(0, 0) , lod) >> 1) << 1);
    float w2 = uintBitsToFloat((texelFetch(tex, 2*texCoord + ivec2(1, 0), lod) >> 1) << 1);
    float w3 = uintBitsToFloat((texelFetch(tex, 2*texCoord + ivec2(0, 1), lod) >> 1) << 1);
    float w4 = uintBitsToFloat((texelFetch(tex, 2*texCoord + ivec2(1, 1), lod) >> 1) << 1);

    float M = max(max(w1, w2), max(w3, w4));
    float m = min(min(w1, w2), min(w3, w4));

    fragmentColor = color;

}

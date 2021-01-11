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
uniform int convertYUV;
uniform int convert565;

in vec2 texCoord;
out vec4 fragmentColor;


void main() {

    vec4 color = vec4(solidFillColor, 0.0);
    uint textureValueInt = texture(tex, texCoord).x;
    uint textureType = textureValueInt & 1u;
    float textureValue = uintBitsToFloat(textureValueInt);


    if (textureMode == 2u || textureMode == textureType) {
        float i = (textureValue - textureMin)/(textureMax - textureMin);
        float k = frequency*i + phase;
        float n = mod(float(numColors - 1)*k, float(numColors - 1));
        int p = int(floor(n));
        float q = mod(n, 1.0);
        color.rgb = (1.0 - q)*palette[p] + q*palette[p + 1];
    }


    if (convertYUV == 1) {

        ivec3 colorInt = ivec3(255.0*color);
        int y = ((66*colorInt.r + 129*colorInt.g + 25*colorInt.b + 128) >> 8) + 16;
        int u = ((-38*colorInt.r - 74*colorInt.g + 112*colorInt.b + 128) >> 8) + 128;
        int v = ((112*colorInt.r - 94*colorInt.g - 18*colorInt.b + 128) >> 8) + 128;
        color.rgb = vec3(y, u, v)/255.0;

    }

//    if (convert565 == 1) {
//
//        uvec3 colorInt = uvec3(255.0*color);
//        uint r = (colorInt.r & 248u) >> 3;
//        uint g = (colorInt.g & 252u) >> 2;
//        uint b = (colorInt.b & 248u) >> 3;
//        uint q = ((b | (g << 5)) | (r << 11));
//        uint high = (q & 65280u) >> 8;
//        uint low = q & 255u;
//        color= vec4(low, high, 0.0, 0.0)/255.0;
//
//    }

    fragmentColor = color;

}

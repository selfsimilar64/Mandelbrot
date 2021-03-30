#version 300 es

precision highp float;
precision highp int;

const float specialValue = -1.23456788063;

uniform highp usampler2D tex;
uniform int numColors;
uniform vec3 palette[30];
uniform vec3 accent1;
uniform vec3 accent2;
uniform float frequency;
uniform float phase;
uniform float density;
uniform uint textureMode;
uniform float textureMin;
uniform float textureMax;
uniform int convertYUV;
uniform int convert565;
uniform int imageTexture;

in vec2 texCoord;
out vec4 fragmentColor;


void main() {

    vec4 color = vec4(accent1, 0.0);
    uint textureValueInt = texture(tex, texCoord).x;

    uint textureType = textureValueInt & 1u;   // convergent or divergent

    float textureValue = uintBitsToFloat((textureValueInt >> 1) << 1);


    if (textureMode == 2u || textureMode == textureType) {

        if (textureValue == specialValue) {

            color.rgb = accent2;

        } else {

            float i = (textureValue - textureMin)/(textureMax - textureMin);
            float j = pow(abs(i), 1.0/pow(4.0, density));
            float k = frequency*j + phase;
            float n = float(numColors - 1)*fract(k);
            int p = int(floor(n));
            float q = mod(n, 1.0);
            color.rgb = mix(palette[p], palette[p + 1], q);

        }

    } else color.rgb = accent1;


    if (convertYUV == 1) {

        ivec3 colorInt = ivec3(255.0*color);
        int y = ((66*colorInt.r + 129*colorInt.g + 25*colorInt.b + 128) >> 8) + 16;
        int u = ((-38*colorInt.r - 74*colorInt.g + 112*colorInt.b + 128) >> 8) + 128;
        int v = ((112*colorInt.r - 94*colorInt.g - 18*colorInt.b + 128) >> 8) + 128;
        color.rgb = vec3(y, u, v)/255.0;

    }

    if (imageTexture == 1 && (textureMode == 2u || textureMode == textureType)) {

        uint r = textureValueInt >> 24;
        uint g = (textureValueInt >> 16) & 255u;
        uint b = (textureValueInt >> 8) & 255u;
        uint a = textureValueInt & 254u;
        color = vec4(r, g, b, a)/256.0;

        if (color.a < 1.0) {
            float n = float(numColors - 1)*phase;
            int p = int(floor(n));
            float q = mod(n, 1.0);
            vec4 bgcolor = vec4(mix(palette[p], palette[p + 1], q), 1.0);
            color += bgcolor*(1.0 - color.a);
        }

    }

    if (convert565 == 1) {

        uvec3 colorInt = uvec3(255.0*color);
        uint r = (colorInt.r & 248u) >> 3;
        uint g = (colorInt.g & 252u) >> 2;
        uint b = (colorInt.b & 248u) >> 3;
        uint q = ((b | (g << 5)) | (r << 11));
        uint high = q >> 8;
        uint low = q & 255u;
        color = vec4(low, high, 0.0, 0.0)/255.0;

    }


    fragmentColor = color;

}

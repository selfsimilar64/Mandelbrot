#version 300 es

precision highp float;
precision highp int;

uniform highp usampler2D tex;

in vec2 texCoord;
out uint fragmentColor;

void main() {

    fragmentColor = texture(tex, texCoord).x; // sample texture

}

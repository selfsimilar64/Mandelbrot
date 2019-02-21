#version 320 es

precision highp float;
uniform vec2 screenRes;
uniform sampler2D tex;
in vec2 texCoord;
out vec4 fragmentColor;

void main() {

    fragmentColor = texture(tex, texCoord);         // sample texture
    fragmentColor.a = 1.0;

}

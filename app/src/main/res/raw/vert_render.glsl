#version 300 es

precision highp float;
precision highp int;

uniform float bgScale;

in vec4 viewCoords;
out vec4 viewPos;

void main() {

    gl_Position = viewCoords;
    viewPos = bgScale * viewCoords;

}
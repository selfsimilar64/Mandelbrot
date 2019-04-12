#version 320 es

uniform float bgScale;
in vec4 viewCoords;
out vec4 viewPos;

void main() {

    gl_Position = viewCoords;
    viewPos = bgScale * viewCoords;

}
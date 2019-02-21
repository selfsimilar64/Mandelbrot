#version 320 es

in vec4 viewCoords;

void main() {

    gl_Position = viewCoords;

}
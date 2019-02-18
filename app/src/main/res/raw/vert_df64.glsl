#version 320 es

in vec4 vPos;
uniform mat4 mvpMat;

void main() {

    gl_Position = vPos;

}
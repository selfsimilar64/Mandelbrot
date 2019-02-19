#version 320 es

in vec4 vPos;
out vec2 texCoord;
//uniform mat4 m;

void main() {

    texCoord = 0.5*(vPos.xy + vec2(1.0));
    gl_Position = vPos;

}
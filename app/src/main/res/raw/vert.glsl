#version 320 es

uniform vec2 scale;
uniform vec2 offset;
in vec4 vPosition;
out vec2 C;

void main() {
    C = scale*vPosition.xy + offset;
    gl_Position = vPosition;
}
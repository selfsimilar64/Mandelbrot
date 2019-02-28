#version 320 es

in vec4 viewCoords;
in vec4 quadCoords;
out vec2 texCoord;

void main() {

    texCoord = 0.5*(viewCoords.xy + vec2(1.0));
    gl_Position = quadCoords;

}
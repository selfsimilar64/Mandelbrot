#version 300 es

uniform float yOrient;
uniform float scale;
uniform vec2 shift;

in vec4 viewCoords;
in vec4 quadCoords;
out vec2 texCoord;

void main() {

    texCoord = 0.5*(vec2(viewCoords.x, yOrient*viewCoords.y) + vec2(1.0));
    texCoord = scale*texCoord + shift;
    gl_Position = quadCoords;

}
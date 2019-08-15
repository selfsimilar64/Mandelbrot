#version 320 es

uniform float yOrient;

in vec4 viewCoords;
in vec4 quadCoords;
out vec2 texCoord;

void main() {

    texCoord = 0.5*(vec2(viewCoords.x, yOrient*viewCoords.y) + vec2(1.0));
    gl_Position = quadCoords;

}
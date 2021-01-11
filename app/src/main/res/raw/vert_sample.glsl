#version 300 es

precision highp float;
precision highp int;

uniform float yOrient;
uniform float scale;
uniform vec2 shift;

in vec4 viewCoords;
in vec4 quadCoords;
out vec2 texCoord;

void main() {

    texCoord = scale*0.5*(vec2(viewCoords.x, yOrient*viewCoords.y) + vec2(1.0)) + shift;
    gl_Position = quadCoords;

}
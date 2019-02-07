#version 320 es

in vec4 vPos;

void main() {

    // vec2 xPos = vec2(vPos.x, 0.0);
    // vec2 yPos = vec2(vPos.y, 0.0);

    // C = scale*vPosition.xy + offset;

    // xC = ds_add(ds_mul(xScale, xPos), xOffset);
    // yC = ds_add(ds_mul(yScale, yPos), yOffset);

    gl_Position = vPos;
}
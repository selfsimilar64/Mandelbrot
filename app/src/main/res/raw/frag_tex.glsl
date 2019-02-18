#version 320 es

precision highp float;
uniform vec2 screenRes;
uniform sampler2D tex;
in vec2 texCoord;
out vec4 fragmentColor;

void main() {

    // vec2 texCoord = gl_FragCoord.xy / screenRes;    // range [0, 1]
    fragmentColor = texture(tex, texCoord);         // sample texture
    // fragmentColor.rgb = vec3(gl_FragCoord.x/screenRes.x, gl_FragCoord.y/screenRes.y, 0.0);
    fragmentColor.a = 1.0;

}

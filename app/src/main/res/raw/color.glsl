#version 320 es

precision highp float;
uniform sampler2D tex;
uniform int numColors;
uniform vec3 palette[10];
uniform float frequency;
uniform float phase;
uniform float xTouchPos;
uniform float yTouchPos;
in vec2 texCoord;
out vec4 fragmentColor;

void main() {

    vec3 color = vec3(0.0);
    vec4 s = texture(tex, texCoord);         // sample texture

    if (s.w != -1.0) {

        float n = mod(float(numColors - 1)*(frequency*s.z + phase), float(numColors - 1));
        int p = int(floor(n));
        float q = mod(n, 1.0);
        color = (1.0 - q)*palette[p] + q*palette[p + 1];



        // == NORMAL MAP COLORING ===================================================================

        // calculate rays for lighting calculations
        vec3 normRay = vec3(cos(s.x), sin(s.x), 1.0);
        normRay /= length(normRay);
        float lightHeight = 1.0;
        vec3 lightRay = vec3(xTouchPos, yTouchPos, lightHeight);
        lightRay /= length(lightRay);
        vec3 viewRay = vec3(0.0, 0.0, 1.0);
        vec3 reflectRay = 2.0*dot(normRay, lightRay)*normRay - lightRay;

        // calculate lighting components
        float diffuse_intensity = 0.2;
        float phi = dot(normRay, lightRay) / lightHeight;
        float diffuse = clamp(phi, 0.0, 1.0);
        diffuse = diffuse_intensity*(diffuse - 1.0) + 1.0;

        float specular_intenseity = 0.5;
        float specular_phong = 3.0;
        float alpha = dot(reflectRay, viewRay);
        float specular = clamp(alpha, 0.0, 1.0);
        specular = specular_intenseity*1.5*pow(specular, specular_phong);
        diffuse *= 1.0 - specular;

        color = diffuse*color + specular;

    }



    fragmentColor = vec4(color, 1.0);

}

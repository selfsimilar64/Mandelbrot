#version 320 es

precision highp float;
uniform sampler2D tex;
uniform float frequency;
uniform float phase;
uniform float maxIter;
in vec2 texCoord;
out vec4 fragmentColor;

void main() {

    vec3 color = vec3(0.0);

    vec3 black      =  vec3(0.0, 0.0, 0.0);
    vec3 purple     =  vec3(0.3, 0.0, 0.5);
    vec3 red        =  vec3(1.0, 0.0, 0.0);
    vec3 pink       =  vec3(1.0, 0.3, 0.4);
    vec3 yellow     =  vec3(1.0, 1.0, 0.0);
    vec3 white      =  vec3(1.0, 1.0, 1.0);
    vec3 darkblue   =  vec3(0.0, 0.15, 0.25);
    vec3 orange     =  vec3(1.0, 0.6, 0.0);
    vec3 turquoise  =  vec3(64.0, 224.0, 208.0) / 255.0;
    vec3 magenta    = vec3(1.0, 0.0, 1.0);
    vec3 tusk       = vec3(237.0, 205.0, 185.0) / 256.0;
    vec3 yellowish  = vec3(1.0, 0.95, 0.75);
    vec3 darkblue2  = vec3(0.11, 0.188, 0.35);
    vec3 grass      = vec3(0.313, 0.53, 0.45);


    float num_colors = 5.0;

//    vec3 c1 = vec3(0.0, 0.1, 0.2);
//    vec3 c2 = darkblue;
//    vec3 c3 = black;
//    vec3 c4 = vec3(0.3, 0.0, 0.1);
//    vec3 c5 = white;
//    vec3 c6 = black;

//    vec3 c1 = white;
//    vec3 c2 = purple;
//    vec3 c3 = black;
//    vec3 c4 = vec3(0.8, 0.0, 0.3);
//    vec3 c5 = white;


    vec3 c1 = yellowish;
    vec3 c2 = darkblue2;
    vec3 c3 = black;
    vec3 c4 = 0.9*grass;
    vec3 c5 = pink;

    vec4 s = texture(tex, texCoord);         // sample texture
//    vec2 u = 2.0*s.xy - 1.0;
//    s.x = (s.x + 1.0)/2.0;
//    s.y = (s.y + 1.0)/2.0;

    // s is being clipped to [0, 1] when stored in fragmentColor



//    if (s.w < 1.0) {

        //    float n = mod(frequency*(5.0*s.z + phase), 5.0);
//        float n = mod(frequency*num_colors*(s.z) + phase, num_colors);

    if (s.x != -1.0) {

        float n = s.z;

        if (n >= 0.0 && n < 1.0) { color = (1.0-n) * c1   +   (n)     * c2; }
        else if (n >= 1.0 && n < 2.0) { color = (2.0-n) * c2   +   (n-1.0) * c3; }
        else if (n >= 2.0 && n < 3.0) { color = (3.0-n) * c3   +   (n-2.0) * c4; }
        else if (n >= 3.0 && n < 4.0) { color = (4.0-n) * c4   +   (n-3.0) * c5; }
        else if (n >= 4.0 && n < 5.0) { color = (5.0-n) * c5   +   (n-4.0) * c1; }
        //    else if (n >= 5.0 && n < 6.0) {  color = (6.0-n) * c6   +   (n-5.0) * c1;  }

    }



        // == NORMAL MAP COLORING ===================================================================

        // calculate rays for lighting calculations
//        vec3 normRay = vec3(u.x, u.y, 1.0);
//        normRay /= length(normRay);
//        vec3 lightRay = vec3(1.0, 1.0, 1.25);
//        lightRay /= length(lightRay);
//        vec3 viewRay = vec3(0.0, 0.0, 1.0);
//        vec3 reflectRay = 2.0*dot(normRay, lightRay)*normRay - lightRay;

        // calculate lighting components
//        float diffuse = dot(normRay, lightRay);
//        diffuse /= (1.0 + lightRay.z);
//        if (diffuse < 0.0) { diffuse = 0.0; }
//        float specular = pow(dot(reflectRay, viewRay), 1.5);
//        if (specular < 0.0) { specular = 0.0; }

        //    color = vec3(1.0);
//        color = 1.0*(diffuse + 0.2)*color + 0.9*vec3(specular + 0.01);
        //    color /= s.z;


        //    color = vec3(s.x, 0.0, s.y);
        //    color = vec3(specular);
        //    color = vec3(diffuse);


//    }

//    color = vec3(frequency*s.w + phase);


    fragmentColor = vec4(color, 1.0);

}

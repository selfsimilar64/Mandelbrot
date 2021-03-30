#version 300



void image_loop(vec2 z, inout vec2 outlier) {
    if (outlier == ZERO && cmod(z) > q1.x) outlier = z;
}
float image_final(vec2 z, vec2 z1, vec2 z0, uint n, vec2 outlier, bool textureIn, vec2 alpha, vec2 alpha1) {

    float modz1;
    float div = divergence(z, z1, modz1, textureIn);
    float t = -log(log(modz1)/log(R))/log(div);
    //    t = 0.5*(cos((t + 1.0)*pi) + 1.0);
    //    t = pow(t, q3.x);
    //    vec2 s = (1.0 - t)*z1 + t*z;
    //    vec2 s = z1 + alpha1*t + (3.0*(z - z1) - 2.0*alpha1 - alpha)*t*t + (2.0*(z1 - z) + alpha + alpha1)*t*t*t;

    //    vec4 p = texture(image, abs(R/z)*q1.x + q2);
    //    vec4 p = (1.0 - t)*texture(image, z/q1.x + q2) + t*texture(image, z1/q1.x + q2);
    //    vec4 p = texture(image, vec2(carg(c)*q1.x, t));
    //    vec4 p = texture(image, 0.5*(z*q1.x/R + 1.0) + q2);
    //    vec4 p = texture(image, 0.5*(outlier/q1.x + 1.0)*q3.x + q2);


    //    vec2 w = 0.5*q3.x*(z/q1.x + 1.0);
    vec2 w = vec2(
    carg(z)/pi + 1.0,  // [0, 2]
    q3.x/(float(n) + t)
    );

    //    float phi = outlier.x + q1.x;
    //    float rho = 1.0/(1.0 - outlier.y) - 1.0;
    //    vec2 w = rho*vec2(cos(phi), sin(phi));
    vec4 p = texture(image, w + q2);

    uint r = uint(p.r*254.0);
    uint g = uint(p.g*254.0);
    uint b = uint(p.b*254.0);
    //    uint r = uint(w.y*254.0);
    //    uint g = r;
    //    uint b = r;
    uvec4 up = uvec4(r, g, b, 0u);
    return uintBitsToFloat((up.r << 24) + (up.g << 16) + (up.b << 8) + up.a);

}

void main() {

}

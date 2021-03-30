vec2 c = cmult(vec2(xScale.x, yScale.x)*viewPos.xy, vec2(cosRotate, sinRotate)) + vec2(xCoord.x, yCoord.x);
vec2 z0 = vec2(x0, y0);
vec2 z = z0;
vec2 z1, z2, z3, z4 = vec2(z0);
float modsqrz = 0.0;
float sum, sum1 = 0.0;
float modc = cmod(c);
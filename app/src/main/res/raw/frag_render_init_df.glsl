vec4 c = cadd(cmult(vec2(cosRotate, sinRotate), vec4(mult(viewPos.x, xScale), mult(viewPos.y, yScale))), vec4(xCoord, yCoord));
vec4 z0 = vec4(vec2(x0, 0.0), vec2(y0, 0.0));
vec4 z = z0;
vec4 z1, z2, z3, z4 = vec4(z0);
vec2 modsqrz = vec2(0.0);
vec2 sum, sum1 = _0;
vec2 modc = cmod(c);
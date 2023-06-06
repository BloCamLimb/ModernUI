#version 330 core

layout(std140) uniform SmoothBlock {
    float u_SmoothRadius;
};
layout(std140) uniform PaintBlock {
    vec2 u_Bezier0;
    vec2 u_Bezier1;
    vec2 u_Bezier2;
    float u_StrokeRadius;
};

in vec2 f_Position;
in vec4 f_Color;

out vec4 fragColor;

vec3 solveCubic(float a, float b, float c) {
    float p = b - a*a / 3.0, p3 = p*p*p;
    float q = a * (2.0*a*a - 9.0*b) / 27.0 + c;
    float d = q*q + 4.0*p3 / 27.0;
    float offset = -a / 3.0;
    if (d >= 0.0) {
        float z = sqrt(d);
        vec2 x = (vec2(z, -z) - q) / 2.0;
        vec2 uv = sign(x)*pow(abs(x), vec2(1.0/3.0));
        return vec3(offset + uv.x + uv.y);
    }
    float v = acos(-sqrt(-27.0 / p3) * q / 2.0) / 3.0;
    float m = cos(v), n = sin(v)*1.732050808;
    return vec3(m + m, -n - m, n - m) * sqrt(-p / 3.0) + offset;
}

float distanceToBezier(vec2 A, vec2 B, vec2 C, vec2 p) {
    //B = mix(B + vec2(1e-4), B, sign(B * 2.0 - A - C));
    vec2 a = B - A, b = A - B * 2.0 + C, c = a * 2.0, d = A - p;
    vec3 k = vec3(3.*dot(a, b), 2.*dot(a, a)+dot(d, b), dot(d, a)) / dot(b, b);
    vec3 t = clamp(solveCubic(k.x, k.y, k.z), 0.0, 1.0);
    vec2 pos = A + (c + b*t.x)*t.x;
    float dis = length(pos - p);
    pos = A + (c + b*t.y)*t.y;
    dis = min(dis, length(pos - p));
    pos = A + (c + b*t.z)*t.z;
    return min(dis, length(pos - p));
}

void main() {
    // distance to the curve border
    float v = distanceToBezier(u_Bezier0, u_Bezier1, u_Bezier2, f_Position) - u_StrokeRadius;
    // out of the curve, discard it
    if (v >= 0.0) discard;
    fragColor = f_Color * (1.0 - smoothstep(-u_SmoothRadius, 0.0, v));
}
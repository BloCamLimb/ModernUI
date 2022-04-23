#version 450 core

layout(std140, binding = 1) uniform SmoothBlock {
    float u_SmoothRadius;
};
layout(std140, binding = 3) uniform PaintBlock {
    vec2 u_Bezier0;
    vec2 u_Bezier1;
    vec2 u_Bezier2;
    float u_StrokeRadius;
};

smooth in vec2 f_Position;
smooth in vec4 f_Color;

layout(location = 0) out vec4 fragColor;

float det(vec2 a, vec2 b) {
    return a.x * b.y - b.x * a.y;
}

void main() {
    vec2 b0 = u_Bezier0 - f_Position;
    vec2 b1 = u_Bezier1 - f_Position;
    vec2 b2 = u_Bezier2 - f_Position;

    float a = det(b0, b2), b = 2.0 * det(b1, b0), d = 2.0 * det(b2, b1);
    float f = b * d - a * a;
    vec2 d21 = b2 - b1, d10 = b1 - b0, d20 = b2 - b0;
    vec2 gf = 2.0 * (b * d21 + d * d10 + a * d20);
    gf = vec2(gf.y, -gf.x);
    vec2 pp = -f * gf / dot(gf, gf);
    vec2 d0p = b0 - pp;
    float ap = det(d0p, d20), bp = 2.0 * det(d10, d0p);
    // (note that 2*ap+bp+dp=2*a+b+d=4*area(b0,b1,b2))
    float t = clamp((ap + bp) / (2.0 * a + b + d), 0.0, 1.0);
    d = length(mix(mix(b0, b1, t), mix(b1, b2, t), t));

    a = 1.0 - smoothstep(u_StrokeRadius - u_SmoothRadius, u_StrokeRadius, d);

    fragColor = f_Color * vec4(1.0, 1.0, 1.0, a);
}
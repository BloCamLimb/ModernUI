#version 330 core

layout(std140) uniform SmoothBlock {
    float u_SmoothRadius;
};
layout(std140) uniform PaintBlock {
    vec4 u_InnerRect;
    float u_Radius;
    float u_StrokeRadius;
};

in vec2 f_Position;
in vec4 f_Color;

out vec4 fragColor;

float aastep(float x) {
    vec2 grad = vec2(dFdx(x), dFdy(x));
    float afwidth = 0.7 * length(grad);
    return smoothstep(-afwidth, afwidth, x);
}

void main() {
    vec2 center = (u_InnerRect.xy + u_InnerRect.zw) * 0.5;

    vec2 b = vec2(u_InnerRect.z - u_InnerRect.x, u_InnerRect.w - u_InnerRect.y) * 0.5;
    vec2 d = abs(f_Position - center)-b;
    float dis = length(max(d,0.0)) + min(max(d.x,d.y),0.0);
    dis = abs(dis) - u_StrokeRadius;

    float a = u_SmoothRadius > 0.0
    ? 1.0 - smoothstep(-u_SmoothRadius, 0.0, dis)
    : 1.0 - aastep(dis);

    fragColor = f_Color * a;
}
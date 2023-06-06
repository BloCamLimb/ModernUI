#version 330 core

layout(std140) uniform SmoothBlock {
    float u_SmoothRadius;
};
layout(std140) uniform PaintBlock {
    vec2 u_CenterPos;
    float u_Radius;
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
    float dis = length(f_Position - u_CenterPos) - u_Radius;

    float a = u_SmoothRadius > 0.0
    ? 1.0 - smoothstep(-u_SmoothRadius, 0.0, dis)
    : 1.0 - aastep(dis);

    fragColor = f_Color * a;
}
#version 450 core

layout(std140, binding = 1) uniform SmoothBlock {
    float u_SmoothRadius;
};
layout(std140, binding = 4) uniform PaintBlock {
    vec2 u_CenterPos;
    vec2 u_Radius;
};

smooth in vec2 f_Position;
smooth in vec4 f_Color;

layout(location = 0) out vec4 fragColor;

void main() {
    float v = length(f_Position - u_CenterPos);

    float a1 = smoothstep(u_Radius.x, u_Radius.x + u_SmoothRadius, v);
    float a2 = smoothstep(u_Radius.y - u_SmoothRadius, u_Radius.y, v);
    float a = a1 * (1.0 - a2);

    fragColor = f_Color * a;
}
#version 450 core

layout(std140, binding = 1) uniform SmoothBlock {
    float u_SmoothRadius;
};
layout(std140, binding = 4) uniform PaintBlock {
    vec2 u_CenterPos;
    float u_Radius;
};

smooth in vec2 f_Position;
smooth in vec4 f_Color;

layout(location = 0) out vec4 fragColor;

void main() {
    float v = length(f_Position - u_CenterPos);

    float a = 1.0 - smoothstep(u_Radius - u_SmoothRadius, u_Radius, v);

    fragColor = f_Color * a;
}
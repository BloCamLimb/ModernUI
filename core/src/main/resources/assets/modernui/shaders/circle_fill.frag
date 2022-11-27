#version 450 core

layout(std140, binding = 1) uniform SmoothBlock {
    float u_SmoothRadius;
};
layout(std140, binding = 4) uniform PaintBlock {
    vec2 u_CenterPos;
    float u_Radius;
};

layout(location = 0) smooth in vec2 f_Position;
layout(location = 1) smooth in vec4 f_Color;

layout(location = 0, index = 0) out vec4 fragColor;

void main() {
    float dis = length(f_Position - u_CenterPos) - u_Radius;

    float a = u_SmoothRadius > 0.0
            ? 1.0 - smoothstep(-u_SmoothRadius, 0.0, dis)
            : 1.0 - clamp(dis / fwidth(dis), 0.0, 1.0);

    fragColor = f_Color * a;
}
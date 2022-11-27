#version 450 core

layout(std140, binding = 1) uniform SmoothBlock {
    float u_SmoothRadius;
};
layout(std140, binding = 5) uniform PaintBlock {
    vec4 u_InnerRect;
    float u_Radius;
    float u_StrokeRadius;
};

layout(location = 0) smooth in vec2 f_Position;
layout(location = 1) smooth in vec4 f_Color;

layout(location = 0, index = 0) out vec4 fragColor;

void main() {
    vec2 tl = u_InnerRect.xy - f_Position;
    vec2 br = f_Position - u_InnerRect.zw;

    vec2 dis = max(br, tl);

    float v = length(max(vec2(0.0), dis)) - u_Radius;
    v = abs(v) - u_StrokeRadius;

    float a = u_SmoothRadius > 0.0
            ? 1.0 - smoothstep(-u_SmoothRadius, 0.0, v)
            : 1.0 - clamp(v / fwidth(v), 0.0, 1.0);

    fragColor = f_Color * a;
}
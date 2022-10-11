#version 450 core

layout(std140, binding = 1) uniform SmoothBlock {
    float u_SmoothRadius;
};
layout(std140, binding = 2) uniform PaintBlock {
    vec2 u_CenterPos;
    float u_MiddleAngle;
    float u_SweepAngle;
    float u_Radius;
    float u_StrokeRadius;
};

layout(location = 0) smooth in vec2 f_Position;
layout(location = 1) smooth in vec4 f_Color;

layout(location = 0, index = 0) out vec4 fragColor;

void main() {
    vec2 v = f_Position - u_CenterPos;

    // smoothing normal direction
    float d1 = abs(length(v) - u_Radius) - u_StrokeRadius;
    float a1 = smoothstep(-u_SmoothRadius, 0.0, d1);

    // sweep angle (0,360) in degrees
    float c = cos(u_SweepAngle * 0.00872664626);

    float f = u_MiddleAngle * 0.01745329252;
    // normalized vector from the center to the middle of the arc
    vec2 up = vec2(cos(f), sin(f));

    // smoothing tangent direction
    float d2 = dot(up, normalize(v)) - c;

    // proportional to how much `d2` changes between pixels
    float w = u_SmoothRadius * fwidth(d2);
    float a2 = smoothstep(w * -0.5, w * 0.5, d2);

    // mix alpha value
    float a = (1.0 - a1) * a2;

    fragColor = f_Color * a;
}
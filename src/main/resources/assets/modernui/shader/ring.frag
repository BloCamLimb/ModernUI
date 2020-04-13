#version 430 core

precision highp float;

layout(location = 0) uniform vec2 u_Radius;
layout(location = 1) uniform vec2 u_CenterPos;

in vec2 f_Position;

out vec4 fragColor;

void main() {
    float v = length(f_Position - u_CenterPos);

    float a = min(
        smoothstep(u_Radius.x - 1.0, u_Radius.x, v),
        1.0 - smoothstep(u_Radius.y - 1.0, u_Radius.y, v));

    fragColor = gl_Color * vec4(1.0, 1.0, 1.0, a);
}
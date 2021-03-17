#version 430 core

precision highp float;

layout(location = 0) uniform float u_Radius;
layout(location = 1) uniform vec2 u_CenterPos;

smooth in vec2 f_Position;
smooth in vec4 f_Color;

out vec4 fragColor;

void main() {
    float v = length(f_Position - u_CenterPos);

    float a = 1.0 - smoothstep(u_Radius.x - 1.0, u_Radius.x, v);

    fragColor = f_Color * vec4(1.0, 1.0, 1.0, a);
}
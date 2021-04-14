#version 430 core

precision highp float;

layout(location = 0) uniform vec3 u_Radius;
layout(location = 1) uniform vec4 u_InnerRect;

smooth in vec2 f_Position;
smooth in vec4 f_Color;

out vec4 fragColor;

void main() {

    vec2 tl = u_InnerRect.xy - f_Position;
    vec2 br = f_Position - u_InnerRect.zw;

    vec2 dis = max(br, tl);

    float v = length(max(vec2(0.0), dis)) - u_Radius.x;

    float a = 1.0 - smoothstep(-u_Radius.y, 0.0, abs(v) - u_Radius.z);

    fragColor = f_Color * vec4(1.0, 1.0, 1.0, a);
}
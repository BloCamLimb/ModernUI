#version 430 compatibility

precision highp float;

layout(location = 0) uniform float u_Radius;
layout(location = 1) uniform vec4 u_InnerRect;

in vec2 f_Position;

out vec4 fragColor;

void main() {

    vec2 tl = u_InnerRect.xy - f_Position;
    vec2 br = f_Position - u_InnerRect.zw;

    vec2 dis = max(br, tl);

    float v = length(max(vec2(0.0), dis)) - u_Radius;

    float ar = 0.5 - smoothstep(-0.5, 1.5, v);
    float a = 1.0 - 2.0 * abs(ar);

    fragColor = gl_Color * vec4(1.0, 1.0, 1.0, a);
}
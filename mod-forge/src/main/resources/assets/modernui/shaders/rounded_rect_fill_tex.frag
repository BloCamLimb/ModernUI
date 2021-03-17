#version 430 core

precision highp float;

layout(location = 0) uniform float u_Radius;
layout(location = 1) uniform vec4 u_InnerRect;
layout(location = 2) uniform sampler2D u_Sampler;

smooth in vec2 f_Position;
smooth in vec4 f_Tint;
smooth in vec2 f_TexCoord;

out vec4 fragColor;

void main() {

    vec2 tl = u_InnerRect.xy - f_Position;
    vec2 br = f_Position - u_InnerRect.zw;

    vec2 dis = max(br, tl);

    float v = length(max(vec2(0.0), dis)) - u_Radius;

    float a = 1.0 - smoothstep(-4.0, 0.0, v);

    fragColor = texture(u_Sampler, f_TexCoord) * f_Tint * vec4(1.0, 1.0, 1.0, a);
}
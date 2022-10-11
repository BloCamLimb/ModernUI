#version 450 core

layout(std140, binding = 1) uniform SmoothBlock {
    float u_SmoothRadius;
};
layout(std140, binding = 5) uniform PaintBlock {
    vec4 u_InnerRect;
    float u_Radius;
};

layout(binding = 0) uniform sampler2D u_Sampler;

layout(location = 0) smooth in vec2 f_Position;
layout(location = 1) smooth in vec4 f_Color;
layout(location = 2) smooth in vec2 f_TexCoord;

layout(location = 0, index = 0) out vec4 fragColor;

void main() {
    vec2 tl = u_InnerRect.xy - f_Position;
    vec2 br = f_Position - u_InnerRect.zw;

    vec2 dis = max(br, tl);

    float v = length(max(vec2(0.0), dis)) - u_Radius;

    float a = 1.0 - smoothstep(-u_SmoothRadius, 0.0, v);

    vec4 samp = texture(u_Sampler, f_TexCoord, -0.475);
    samp.rgb *= samp.a;
    fragColor = samp * f_Color * a;
}
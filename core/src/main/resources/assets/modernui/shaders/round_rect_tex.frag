#version 450 core

precision mediump float;

layout(std140, binding = 1) uniform PaintBlock {
    vec4 u_InnerRect;
    vec3 u_Radius;
};

layout(location = 0) uniform sampler2D u_Sampler;

smooth in vec2 f_Position;
smooth in vec4 f_Color;
smooth in vec2 f_TexCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 tl = u_InnerRect.xy - f_Position;
    vec2 br = f_Position - u_InnerRect.zw;

    vec2 dis = max(br, tl);

    float v = length(max(vec2(0.0), dis)) - u_Radius.x;

    float a = 1.0 - smoothstep(-u_Radius.y, 0.0, v);

    fragColor = texture(u_Sampler, f_TexCoord) * f_Color * vec4(1.0, 1.0, 1.0, a);
}
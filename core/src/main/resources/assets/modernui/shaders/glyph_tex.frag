#version 450 core

precision mediump float;

layout(location = 0) uniform sampler2D u_Sampler;

flat in vec4 f_Color;
smooth in vec2 f_TexCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = vec4(f_Color.rgb, texture(u_Sampler, f_TexCoord).a * f_Color.a);
}
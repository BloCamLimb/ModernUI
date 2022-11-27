#version 450 core

layout(binding = 0) uniform sampler2D u_Sampler;

layout(location = 0) flat in vec4 f_Color;
layout(location = 1) smooth in vec2 f_TexCoord;

layout(location = 0, index = 0) out vec4 fragColor;

void main() {
    vec4 texColor = texture(u_Sampler, f_TexCoord, -0.225);
    texColor.rgb *= texColor.a; // premul for bitmap
    fragColor = texColor * f_Color; // modulate
}
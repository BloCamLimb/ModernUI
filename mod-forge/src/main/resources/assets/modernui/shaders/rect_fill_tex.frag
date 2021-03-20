#version 430 core

precision highp float;

layout(location = 0) uniform sampler2D u_Sampler;

smooth in vec4 f_Tint;
smooth in vec2 f_TexCoord;

out vec4 fragColor;

void main() {
    fragColor = texture(u_Sampler, f_TexCoord) * f_Tint * vec4(1.0, 1.0, 1.0, a);
}
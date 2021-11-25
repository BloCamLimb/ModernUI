#version 450 core

layout(location = 0) uniform sampler2D u_Sampler;

smooth in vec4 f_Color;
smooth in vec2 f_TexCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = texture(u_Sampler, f_TexCoord) * f_Color;
}
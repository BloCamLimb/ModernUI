#version 330 core

uniform sampler2D u_Sampler;

in vec4 f_Color;
in vec2 f_TexCoord;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(u_Sampler, f_TexCoord);
    fragColor = texColor * f_Color;
}
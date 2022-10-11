#version 450 core

layout(binding = 0) uniform sampler2DMS u_Sampler;

layout(location = 1) smooth in vec4 f_Color;
layout(location = 2) smooth in vec2 f_TexCoord;

layout(location = 0, index = 0) out vec4 fragColor;

void main() {
    vec4 color = vec4(0.0);
    ivec2 coord = ivec2(f_TexCoord * textureSize(u_Sampler));
    color += texelFetch(u_Sampler, coord, 0);
    color += texelFetch(u_Sampler, coord, 1);
    color += texelFetch(u_Sampler, coord, 2);
    color += texelFetch(u_Sampler, coord, 3);

    fragColor = (color / 4.0) * f_Color;
}
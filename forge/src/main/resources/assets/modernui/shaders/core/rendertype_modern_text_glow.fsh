#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = vec4(0.0);

    vec2 oneTexel = 1.0 / textureSize(Sampler0, 0);
    color += textureLod(Sampler0, texCoord0 - oneTexel, 1.0) * 0.13533528323; // e^(-2)
    color += textureLod(Sampler0, texCoord0 + oneTexel * vec2(0.0, -1.0), 1.0) * 0.36787944117; // e^(-1)
    color += textureLod(Sampler0, texCoord0 + oneTexel * vec2(1.0, -1.0), 1.0) * 0.13533528323; // e^(-2)
    color += textureLod(Sampler0, texCoord0 + oneTexel * vec2(-1.0, 0.0), 1.0) * 0.36787944117; // e^(-1)
    color += textureLod(Sampler0, texCoord0, 1.0);
    color += textureLod(Sampler0, texCoord0 + oneTexel * vec2(1.0, 0.0),  1.0) * 0.36787944117; // e^(-1)
    color += textureLod(Sampler0, texCoord0 + oneTexel * vec2(-1.0, 1.0), 1.0) * 0.13533528323; // e^(-2)
    color += textureLod(Sampler0, texCoord0 + oneTexel * vec2(0.0, 1.0),  1.0) * 0.36787944117; // e^(-1)
    color += textureLod(Sampler0, texCoord0 + oneTexel, 1.0) * 0.13533528323; // e^(-2)
    color /= 3.0128588976; // 4 * (e^(-1) + e^(-2)) + 1

    color = color * color * (3.0 - 2.0 * color); // smoothstep
    color *= vertexColor * ColorModulator; // multiply
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}

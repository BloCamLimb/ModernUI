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
    vec4 texColor = texture(Sampler0, texCoord0, -0.475);

    // apply distance field
    float x = texColor.a - 0.5;
    vec2 grad = vec2(dFdx(x), dFdy(x));
    float afwidth = 0.7 * length(grad);
    // use 0.05 threshold to get heavier strokes
    // Minecraft uses non-premultiplied alpha blending
    texColor.a = smoothstep(-afwidth - 0.05, afwidth - 0.05, x);

    vec4 color = texColor * vertexColor * ColorModulator;
    if (color.a < 0.01) discard; // requires alpha test
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}

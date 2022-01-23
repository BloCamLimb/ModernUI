#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 BlurDir;
uniform float Progress;

out vec4 fragColor;

void main() {
    vec4 blur = vec4(0.0);

    float radius = floor(Progress);
    vec2 dir = oneTexel * BlurDir;
    for (float r = -radius; r <= radius; r += 1.0) {
        blur += texture(DiffuseSampler, texCoord + r * dir);
    }

    fragColor = vec4(blur.rgb / (radius * 2.0 + 1.0), 1.0);
}
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
    // sigma = radius * 0.5
    // base = -0.5 / (sigma * sigma)
    // factor = 1.0 / (sigma * sqrt(2*PI))
    float base = -2.0 / (radius * radius);
    float factor = 0.7978845608 / radius;
    float randScale = min(radius * 0.2, 1.0);
    vec2 dir = oneTexel * BlurDir;
    float sum = 0.0, f, rand;
    for (float r = -radius; r <= radius; r += 1.0) {
        f = exp(r * r * base) * factor;
        rand = fract(sin(dot(texCoord, vec2(12.9898, 78.233))) * 43758.5453123);
        blur += texture(DiffuseSampler, texCoord + (r * 2.0 + randScale * rand) * dir) * f;
        sum += f;
    }

    fragColor = vec4(blur.rgb / sum, 1.0);
}
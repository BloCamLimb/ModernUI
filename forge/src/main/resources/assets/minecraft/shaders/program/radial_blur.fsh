#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

void main() {
    vec4 blur = vec4(0.0);

    vec2 uv = texCoord;
    uv -= 0.5;
    for (float r = 0; r < 10.0; r += 1.0) {
        blur += texture(DiffuseSampler, uv * (0.9 + 0.011 * r) + 0.5);
    }

    fragColor = vec4(blur.rgb / 10.0, 1.0);
}
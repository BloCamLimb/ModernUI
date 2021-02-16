#version 120

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

uniform vec2 InSize;

uniform vec2 BlurDir;
uniform float Progress;

void main() {
    vec4 blurred = vec4(0.0);

    float radius = floor(Progress);
    for (float r = -radius; r <= radius; r += 1.0) {
        blurred = blurred + texture2D(DiffuseSampler, texCoord + oneTexel * r * BlurDir);
    }

    gl_FragColor = vec4(blurred.rgb / (radius * 2.0 + 1.0), 1.0);
}
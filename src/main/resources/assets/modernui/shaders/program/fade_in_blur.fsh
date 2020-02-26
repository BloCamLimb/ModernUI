#version 120

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

uniform vec2 InSize;

uniform vec2 BlurDir;
uniform float Progress;

void main() {
    vec4 blurred = vec4(0.0);

    float aa = 0.0;
    float pr = floor(3.0 * Progress);

    for(float r = -pr; r <= pr; r += 1.0) {
        vec4 sample0 = texture2D(DiffuseSampler, texCoord + oneTexel * r * BlurDir);
        aa = aa + sample0.a;
        blurred = blurred + sample0;
    }

    gl_FragColor = vec4(blurred.rgb / (pr * 2.0 + 1.0), aa);
}
#version 450

layout(location = 0) uniform sampler2D u_Sampler;

smooth in vec2 f_TexCoord;

layout(location = 0) out vec4 fragColor;

// need update
// for performance, textureLod 1.0f and then samples count can be 1/4 of this
// but need generate mipmap level at least to 1

vec4 blur(vec2 oneTexel, int samples) {
    vec4 O = vec4(0.0);
    float r = float(samples)*0.5;
    float sigma = r*0.5;
    float f = 1./(6.28318530718*sigma*sigma);

    int s2 = samples*samples;
    for (int i = 0; i<s2; i++) {
        vec2 d = vec2(i%samples, i/samples) - r;
        O += texture(u_Sampler, f_TexCoord + oneTexel * d) * exp(-0.5 * dot(d/=sigma, d)) * f;
    }
    // use pre-multiplied alpha
    return O/O.a;
}

void main() {
    // samples is the side length of sampling square (i.e. half of blur radius)
    fragColor = blur(1./iChannelResolution[0].xy, 20);
}
#version 450

layout(location = 0) uniform sampler2D u_Sampler;

smooth in vec2 f_TexCoord;

layout(location = 0) out vec4 fragColor;

#define PI2 6.2831853072 // PI * 2

// need update
// FAST: lod4 loss3 samples12 radius32

void main() {
    vec2 pixel = 1.0 / iResolution.xy;

    float count = 1.0;
    vec4 color = texture(u_Sampler, uv);
    float directionStep = PI2 / 48;

    vec2 off;
    float c, s, dist, dist2, weight;
    for (float d = 0.0; d < PI2; d += directionStep) {
        c = cos(d);
        s = sin(d);
        dist = 1.0 / max(abs(c), abs(s));
        dist2 = dist * 3.0;
        off = vec2(c, s);
        for (float i = dist2; i <= 32.0; i += dist2) {
            weight = i / radius;
            count += weight;
            color += texture(u_Sampler, f_TexCoord + off * pixel * i) * weight;
        }
    }

    fragColor = color / count;
}
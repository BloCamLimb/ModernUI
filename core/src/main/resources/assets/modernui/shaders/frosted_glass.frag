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


    // shadertoy
    #define PI2 6.2831853072 // PI * 2
    #define PI_O_2 1.5707963268 // PI / 2

const float passes = 32.0;
const float radius = 40.0;

void mainImage(out vec4 fragColor, in vec2 fragCoord) {

    vec2 uv = fragCoord / iResolution.xy;

    vec4 color = texture(iChannel0, uv);
    float v = length(fragCoord/iResolution.y - vec2(0.3*iResolution.x/iResolution.y,0.5)) - 0.25;

    if (v>0.0) {
        fragColor = color;
    } else {
        vec2 pixel = 1.0 / iResolution.xy;
        float count = 1.0;
        float directionStep = PI2 / passes;

        vec2 off;
        float c, s, dist, dist2, weight;
        for (float d = 0.0; d < PI2; d += directionStep) {
            c = cos(d);
            s = sin(d);
            dist = 1.0 / max(abs(c), abs(s));
            dist2 = dist * 6.0;
            off = vec2(c, s);
            for (float i = dist2; i <= radius; i += dist2) {
                weight = i / radius;
                count += weight;
                color += textureLod(iChannel0, uv + off * pixel * i, 4.0) * weight;
            }
        }

        fragColor = color / count;
    }
}

// Another version
    #define PI2 6.2831853072 // PI * 2
    #define PI_O_2 1.5707963268 // PI / 2

const float passes = 12.0;
const float radius = 32.0;
const float lossiness = 2.0;
const float preserveOriginal = 0.0;

void mainImage(out vec4 fragColor, in vec2 fragCoord) {

    vec2 uv = fragCoord / iResolution.xy;

    vec4 color = texture(iChannel0, uv);
    //float v = length(fragCoord/iResolution.y - vec2(0.4*iResolution.x/iResolution.y,0.5)) - 0.35;

    if (uv.x>0.5) {
        fragColor = color;
    } else {
        vec2 pixel = 1.0 / iResolution.xy;
        float count = 1.0;
        float directionStep = PI2 / passes;

        vec2 off;
        float c, s, dist, dist2, weight;
        for (float d = 0.0; d < PI2; d += directionStep) {
            c = cos(d);
            s = sin(d);
            dist = 1.0 / max(abs(c), abs(s));
            dist2 = dist * 6.0;
            off = vec2(c, s);
            for (float i = dist2; i <= radius; i += dist2) {
                weight = i / radius;
                count += weight;
                color += textureLod(iChannel0, uv + off * pixel * i, 4.0) * weight;
            }
        }

        fragColor = mix(vec4(0.08), color / count, 0.25);
    }
}

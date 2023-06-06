#version 450 core

layout(std140, binding = 1) uniform SmoothBlock {
    float u_SmoothRadius;
};
layout(std140, binding = 5) uniform PaintBlock {
    vec4 u_Points;
    uvec4 u_Colors;
};

layout(location = 0) smooth in vec2 f_Position;
layout(location = 1) smooth in vec4 f_Color;

layout(location = 0, index = 0) out vec4 fragColor;

float aastep(float x) {
    vec2 grad = vec2(dFdx(x), dFdy(x));
    float afwidth = 0.7 * length(grad);
    return smoothstep(-afwidth, afwidth, x);
}

float sdBox(vec2 p, vec2 b) {
    vec2 d = abs(p)-b;
    return length(max(d,0.0)) + min(max(d.x,d.y),0.0);
}

vec4 normAndPremulColorToLinear(uint color) {
    vec3 r;
    float a = ((color >> 24) & 0xFFu) / 255.0;
    r.b = (color & 0xFFu) / 255.0 * a;
    r.g = ((color >> 8) & 0xFFu) / 255.0 * a;
    r.r = ((color >> 16) & 0xFFu) / 255.0 * a;
    return vec4(pow(r, vec3(2.2)), a);
}

void main() {
    vec2 center = vec2(u_Points.x + u_Points.z, u_Points.y + u_Points.w) * 0.5;
    vec2 size = vec2(u_Points.z - u_Points.x, u_Points.w - u_Points.y) * 0.5;
    float dis = sdBox(f_Position - center, size);

    float a = u_SmoothRadius > 0.0
    ? 1.0 - smoothstep(-u_SmoothRadius, 0.0, dis)
    : 1.0 - aastep(dis);

    vec2 interp = (f_Position - u_Points.xy) / (u_Points.zw - u_Points.xy);

    vec4 q11 = normAndPremulColorToLinear(u_Colors[0]);
    vec4 q21 = normAndPremulColorToLinear(u_Colors[1]);
    vec4 q12 = normAndPremulColorToLinear(u_Colors[3]);
    vec4 q22 = normAndPremulColorToLinear(u_Colors[2]);
    vec4 col = mix(mix(q11,q21,interp.x),mix(q12,q22,interp.x),interp.y);
    col.xyz = pow(col.xyz, vec3(1.0/2.2));

    fragColor = f_Color * col * a;
}

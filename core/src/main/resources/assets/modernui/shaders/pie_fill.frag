#version 450 core

layout(std140, binding = 1) uniform SmoothBlock {
    float u_SmoothRadius;
};
layout(std140, binding = 2) uniform PaintBlock {
    vec2 u_CenterPos;
    float u_MiddleAngle;
    float u_SweepAngle;
    float u_Radius;
};

layout(location = 0) smooth in vec2 f_Position;
layout(location = 1) smooth in vec4 f_Color;

layout(location = 0, index = 0) out vec4 fragColor;

vec2 rotate2D(vec2 p, float a) {
    float s=sin(a),c=cos(a);
    return mat2(c,s,-s,c)*p;
}

float aastep(float x) {
    vec2 grad = vec2(dFdx(x), dFdy(x));
    float afwidth = 0.7 * length(grad);
    return smoothstep(-afwidth, afwidth, x);
}

// by iq
float sdPie(vec2 p, vec2 c, float r) {
    p.x = abs(p.x);
    float l = length(p) - r;
    float m = length(p - c*clamp(dot(p,c),0.0,r) );
    return max(l,m*sign(c.y*p.x-c.x*p.y));
}

void main() {
    vec2 v = f_Position - u_CenterPos;
    float ang = u_SweepAngle * 0.00872664626;
    vec2 p = rotate2D(v, 1.570796326 - u_MiddleAngle * 0.01745329252);
    float dis = sdPie(p, vec2(sin(ang), cos(ang)), u_Radius);

    float a = u_SmoothRadius > 0.0
    ? 1.0 - smoothstep(-u_SmoothRadius, 0.0, dis)
    : 1.0 - aastep(dis);

    fragColor = f_Color * a;
}
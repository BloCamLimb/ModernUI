#version 430 core

precision highp float;

layout(location = 0) uniform float u_Radius;
layout(location = 1) uniform vec4 u_InnerRect;

in vec2 f_Position;

out vec4 fragColor;

void main() {

    vec2 tl = u_InnerRect.xy - f_Position;
    vec2 br = f_Position - u_InnerRect.zw;

    vec2 dis = max(br, tl);
    float mDis = max(dis.x, dis.y);

    float v;
    if (mDis <= 0.0) {
        v = mDis - u_Radius;
    } else {
        v = length(max(vec2(0.0), dis)) - u_Radius;
    }

    float a = 1.0 - smoothstep(0.0, 1.0, v);

    fragColor = gl_Color * vec4(1.0, 1.0, 1.0, a);
}
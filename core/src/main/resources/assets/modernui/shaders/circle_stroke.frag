#version 450 core

precision mediump float;

layout(std140, binding = 2) uniform PaintBlock {
    vec4 u_Radius;
    vec2 u_CenterPos;
};

smooth in vec2 f_Position;
smooth in vec4 f_Color;

layout(location = 0) out vec4 fragColor;

void main() {
    float v = length(f_Position - u_CenterPos);

    /*float a = min(
    smoothstep(u_Radius.x - 1.0, u_Radius.x, v),
    smoothstep(u_Radius.y, u_Radius.y - 1.0, v));*/

    float a = smoothstep(u_Radius.x, u_Radius.x + u_Radius.z, v) * (1.0 - smoothstep(u_Radius.y - u_Radius.z, u_Radius.y, v));

    fragColor = f_Color * vec4(1.0, 1.0, 1.0, a);
}
#version 430 compatibility

precision highp float;

layout(location = 0) uniform vec2 u_Radius;
layout(location = 1) uniform vec2 u_CenterPos;

smooth in vec2 f_Position;
smooth in vec4 f_Color;

out vec4 fragColor;

void main() {
    float v = length(f_Position - u_CenterPos);

    // Method 1
    float a = min(
    smoothstep(u_Radius.x - 1.0, u_Radius.x, v),
    smoothstep(u_Radius.y, u_Radius.y - 1.0, v));

    // Method 2
    //float a = smoothstep(u_Radius.x - 1.0, u_Radius.x, v) * smoothstep(u_Radius.y, u_Radius.y - 1.0, v);

    fragColor = f_Color * vec4(1.0, 1.0, 1.0, a);
}
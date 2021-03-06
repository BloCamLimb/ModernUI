#version 450 core

precision mediump float;

layout(std140, binding = 3) uniform PaintBlock {
    vec4 u_Radius;
    vec2 u_CenterPos;
    vec2 u_Angle;
};

smooth in vec2 f_Position;
smooth in vec4 f_Color;

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 dir = f_Position - u_CenterPos;

    // smoothing normal direction
    float d1 = length(dir) - u_Radius.x;
    float a1 = smoothstep(-u_Radius.y, 0.0, d1);

    // angle (0,360) in degrees
    float c = cos(u_Angle.y * 0.00872664626);

    // direction vector from the circle origin to the middle of the arc
    float f = u_Angle.x * 0.01745329252;
    vec2 up = vec2(cos(f), sin(f));

    // smoothing tangent direction
    float d2 = dot(up, normalize(dir)) - c;

    // proportional to how much `d2` changes between pixels
    float w = u_Radius.y * fwidth(d2);
    float a2 = smoothstep(w * -0.5, w * 0.5, d2);

    // mix alpha value
    float a = (1.0 - a1) * a2;

    fragColor = f_Color * vec4(1.0, 1.0, 1.0, a);
}
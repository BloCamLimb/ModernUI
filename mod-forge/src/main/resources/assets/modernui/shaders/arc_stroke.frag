#version 430 core

precision highp float;

layout(location = 0) uniform vec2 u_Radius;
layout(location = 1) uniform vec2 u_CenterPos;

smooth in vec2 f_Position;
smooth in vec4 f_Color;

out vec4 fragColor;

void main() {
    // center image, correct aspect ratio and zoom out slightly
    vec2 uv = 2.0 * fragCoord.xy / iResolution.xy - 1.0;
    uv.x *= iResolution.x / iResolution.y;
    uv *= 1.1;

    // radius of circle
    float r = 1.00;

    // half thickness of circle
    float t = 0.06;

    // direction vector from the circle origin to the middle of the arc
    vec2 up = vec2(1.0, 1.0);

    // angle (0,360) in degrees
    float c = cos(200.0 * 0.00872664626);

    // smoothing normal direction
    float d1 = abs(length(uv) - r) - t;
    float w1 = 8.0 * fwidth(d1); // proportional to how much `d1` change between pixels
    float s1 = smoothstep(0.0, -w1, d1);

    // smoothing tangent direction
    float d2 = dot(up, normalize(uv)) - c;
    float w2 = 4.0 * fwidth(d2); // proportional to how much `d2` changes between pixels
    float s2 = smoothstep(w2, -w2, d2);

    // mix perpendicular and parallel smoothing
    float s = s1 * (1.0 - s2);

    // coloring
    vec3 lineColor = vec3(0.2, 0.8, 0.9);
    vec3 bgColor   = vec3(0.0, 0.0, 0.0);

    fragColor = vec4((1.0 - s) * bgColor + s * lineColor, 1.0);
}
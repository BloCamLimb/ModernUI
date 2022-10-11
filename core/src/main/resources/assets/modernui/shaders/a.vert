// Copyright (C) 2022 BloCamLimb. All rights reserved.
#version 450 core

layout(std140, binding = 0) uniform MatrixBlock {
    mat4 u_Projection;
    mat3x4 u_ModelView;
    int u_Type;
};

layout(location = 0) in vec2 a_Pos;
layout(location = 1) in vec4 a_Color;

smooth out vec2 f_Position;
smooth out vec4 f_Color;

void main() {
    f_Position = a_Pos;
    f_Color = a_Color;

    switch (u_Type) {
        case 0: {
            break;
        }
        case 1: {
            break;
        }
    }

    gl_Position = u_Projection * u_ModelView * vec3(a_Pos, 0.0);
}

// glow wave
float rand(vec2 n)
{
    return fract(sin(dot(n, vec2(12.9898,12.1414))) * 83758.5453);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    vec2 uv = fragCoord/iResolution.xy;
    vec2 pos = 2.0 * uv - 1.0;
    pos.y /= iResolution.x/iResolution.y;

    float dist = abs(pos.y-sin(pos.x*10.0-iTime*5.0)*0.1-cos(pos.x*5.0)*0.05);
    dist = pow(0.1/dist,0.8);

    vec4 col = vec4(mix(vec3(0.2,0.85,0.95),vec3(0.85,0.5,0.75),pos.x*0.5+0.5),1.0);
    col *= (dist+rand(pos.yx)*0.05);
    col = 1.0 - exp(-col*0.5);

    vec4 dst = texture(iChannel0, uv);
    vec4 src = vec4(col);

    fragColor = src + dst * (1.0 - src.a);
}

// sweep gradient
float rand(vec2 n)
{
    return fract(sin(dot(n, vec2(12.9898,12.1414))) * 83758.5453);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    vec2 uv = fragCoord/iResolution.xy;
    vec2 pos = 2.0 * uv - 1.0;
    pos.y /= iResolution.x/iResolution.y;

    float t = 2.0 * abs(atan(-pos.y, -pos.x) * 0.1591549430918);

    vec3 from = pow(vec3(0.2,0.85,0.95),vec3(2.2));
    vec3 to = pow(vec3(0.85,0.5,0.75),vec3(2.2));
    vec3 col = mix(from,to,t);
    col = pow(col,vec3(1.0/2.2));
    col += (rand(pos)-0.5) / 255.0;

    fragColor = vec4(col,1.0);
}
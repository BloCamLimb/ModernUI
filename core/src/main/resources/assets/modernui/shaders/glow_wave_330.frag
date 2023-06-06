// Copyright (C) 2022 BloCamLimb. All rights reserved.
#version 330 core

layout(std140) uniform MatrixBlock {
    mat4 u_Projection;
    mat4 u_ModelView;
    vec4 u_Color;
};

in vec2 f_Position;
in vec4 f_Color;

out vec4 fragColor;

float rand(vec2 n) {
    return fract(sin(dot(n, vec2(12.9898,12.1414))) * 83758.5453);
}

void main() {
    vec2 pos = f_Position;

    float dist = abs(pos.y-sin(pos.x*10.0-u_Color.x*5.0)*0.1-cos(pos.x*5.0)*0.05);
    dist = pow(0.1/dist,0.8);

    vec4 col = vec4(mix(vec3(0.2,0.85,0.95),vec3(0.85,0.5,0.75),pos.x*0.5+0.5),1.0);
    col *= (dist+rand(pos.yx)*0.05);
    col = 1.0 - exp(-col*0.5);

    fragColor = col;
}
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

// linear gradient
float rand(vec2 n)
{
    return fract(sin(dot(n, vec2(12.9898,12.1414))) * 83758.5453);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    vec2 uv = fragCoord/iResolution.xy;
    vec2 pos = 2.0 * uv - 1.0;
    float aspect = iResolution.x/iResolution.y;
    pos.y /= aspect;

    float t = uv.x;

    vec3 from = pow(vec3(0.2,0.85,0.95),vec3(2.2));
    vec3 to = pow(vec3(0.85,0.5,0.75),vec3(2.2));
    vec3 col = mix(from,to,t);
    col = pow(col,vec3(1.0/2.2));
    col += (rand(pos)-0.5) / 255.0;

    fragColor = vec4(col,1.0);
}

// radial gradient
float rand(vec2 n)
{
    return fract(sin(dot(n, vec2(12.9898,12.1414))) * 83758.5453);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    vec2 uv = fragCoord/iResolution.xy;
    vec2 pos = 2.0 * uv - 1.0;
    float aspect = iResolution.x/iResolution.y;
    pos.y /= aspect;

    float t = length(pos)*aspect;

    vec3 from = pow(vec3(0.2,0.85,0.95),vec3(2.2));
    vec3 to = pow(vec3(0.85,0.5,0.75),vec3(2.2));
    vec3 col = mix(from,to,t);
    col = pow(col,vec3(1.0/2.2));
    col += (rand(pos)-0.5) / 255.0;

    float r = 1.0/aspect;
    float a = 1.0 - smoothstep(r-0.01,r,length(pos));
    col *= a;

    fragColor = vec4(col,1.0);
}

// sweep gradient, angle gradient, angular gradient, conic gradient, conical gradient
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

// bilinear gradient / quad gradient / rectangular gradient / four color gradient
float rand(vec2 n)
{
    return fract(sin(dot(n, vec2(12.9898,12.1414))) * 83758.5453);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    vec2 uv = fragCoord/iResolution.xy;
    vec2 pos = 2.0 * uv - 1.0;
    pos.y /= iResolution.x/iResolution.y;

    float t1 = uv.x;
    float t2 = 1.0-uv.y;

    const vec3 q11 = pow(vec3(0.2,0.85,0.95),vec3(2.2)); // top left
    const vec3 q21 = pow(vec3(0.85,0.5,0.75),vec3(2.2)); // top right
    const vec3 q12 = pow(vec3(0.95,0.5,0.05),vec3(2.2)); // bottom left
    const vec3 q22 = pow(vec3(0.75,0.95,0.7),vec3(2.2)); // bottom right
    vec3 col = mix(mix(q11,q21,t1),mix(q12,q22,t1),t2);
    col = pow(col,vec3(1.0/2.2));
    col += (rand(pos)-0.5) / 255.0;

    fragColor = vec4(col,1.0);
}

// diamond gradient
float rand(vec2 n)
{
    return fract(sin(dot(n, vec2(12.9898,12.1414))) * 83758.5453);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    vec2 uv = fragCoord/iResolution.xy;
    vec2 pos = 2.0 * uv - 1.0;
    float aspect = iResolution.x/iResolution.y;
    pos.y /= aspect;

    vec2 v = abs(pos);
    float t = (v.x+v.y)*aspect;

    float a = 1.0 - smoothstep(0.99,1.0,t);

    vec3 from = pow(vec3(0.2,0.85,0.95),vec3(2.2));
    vec3 to = pow(vec3(0.85,0.5,0.75),vec3(2.2));
    vec3 col = mix(from,to,t);
    col = pow(col,vec3(1.0/2.2));
    col += (rand(pos)-0.5) / 255.0;
    col *= a;

    fragColor = vec4(col,1.0);
}

// two circle conical
// polar gradient
// barycentric gradient


// test SDF with anti-alias
const vec3 col1 = vec3(239.,202.,195.)/255.;
const vec3 col2 = vec3(240.,227.,225.)/255.;

float sdBox( in vec2 p, in vec2 b )
{
    vec2 d = abs(p)-b;
    //return max(d.x,d.y);
    return length(max(d,0.0)) + min(max(d.x,d.y),0.0);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 uv = fragCoord/iResolution.xy;
    vec2 pos = 2.0 * uv - 1.0;
    pos.y /= iResolution.x/iResolution.y;

    //float dis = length(pos)-0.4;
    //float dis = dot(abs(pos),vec2(1.0))-0.4;
    float dis = sdBox(pos,vec2(0.4));

    vec3 col = mix(col1,col2,1.-clamp(dis/fwidth(dis),0.,1.));

    fragColor = vec4(col,1.0);
}
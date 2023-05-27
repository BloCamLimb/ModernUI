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
const vec3 col1 = vec3(239,202,195)/255.;
const vec3 col2 = vec3(240,227,225)/255.;

vec2 rotate2D(vec2 p, float a)
{
    float s=sin(a),c=cos(a);
    return mat2(c,s,-s,c)*p;
}

float sdBox( in vec2 p, in vec2 b )
{
    vec2 d = abs(p)-b;
    //return max(d.x,d.y);
    return length(max(d,0.0)) + min(max(d.x,d.y),0.0);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 uv = fragCoord/iResolution.xy;
    vec2 pos = 2.0*uv-1.0;
    pos.y *= iResolution.y/iResolution.x;

    pos = rotate2D(pos,0.17453292519943295);

    //float dis = length(pos)-0.4;
    //float dis = dot(abs(pos),vec2(1.0))-0.4;
    float dis = sdBox(pos,vec2(0.4));

    //vec3 col = mix(col1,col2,1.-clamp(dis/fwidth(dis),0.,1.));
    float edge = uv.x>.5?clamp(1.3*dis/fwidth(dis)+0.65,0.,1.):step(0.,dis);

    //fragColor = vec4(col,1.0);
    fragColor = vec4(1.0,edge,edge,1.0);
}

// final AA rectangle
const vec3 col1 = vec3(239,202,195)/255.;

vec2 rotate2D(vec2 p, float a)
{
    float s=sin(a),c=cos(a);
    return mat2(c,s,-s,c)*p;
}

float sdBox(vec2 p, vec2 b)
{
    p = abs(p)-b;
    return max(p.x,p.y);
}

float aastep(float f)
{
    vec2 grad = vec2(dFdx(f), dFdy(f));
    float afwidth = 0.7 * length(grad); // SK_DistanceFieldAAFactor
    return smoothstep(-afwidth, afwidth, f);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 uv = fragCoord/iResolution.xy;
    vec2 pos = 2.0*uv-1.0;
    pos.y *= iResolution.y/iResolution.x;

    pos = rotate2D(pos,sin(iTime*2.0)*3.1415926535);

    float dis = sdBox(pos,vec2(0.2));
    vec3 col = vec3(1.0,0.9,1.0) + sign(dis)*vec3(-0.3,0.4,0.3);
    col *= 1.0 - exp(-6.0*abs(dis));
    col *= 0.8 + 0.2*cos(300.0*dis);
    dis = abs(dis)-0.025;
    col = mix( col, col1, 1.0-(uv.x>0.5?aastep(dis):step(0.0,dis)));

    fragColor = vec4(col,1.0);
}

// two circle light
vec4 uncharted2_tonemap_partial(vec4 x)
{
    float A = 0.15f;
    float B = 0.50f;
    float C = 0.10f;
    float D = 0.20f;
    float E = 0.02f;
    float F = 0.30f;
    return ((x*(A*x+C*B)+D*E)/(x*(A*x+B)+D*F))-E/F;
}

vec4 uncharted2_filmic(vec4 v)
{
    float exposure_bias = 2.0f;
    vec4 curr = uncharted2_tonemap_partial(v * exposure_bias);

    vec4 W = vec4(11.2f);
    vec4 white_scale = vec4(1.0f) / uncharted2_tonemap_partial(W);
    return curr * white_scale;
}

float luminance(vec3 v)
{
    return dot(v, vec3(0.2126f, 0.7152f, 0.0722f));
}

float rand(vec2 n)
{
    return fract(sin(dot(n, vec2(12.9898,12.1414))) * 83758.5453);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 uv = fragCoord/iResolution.xy;
    vec2 pos = 2.0 * uv - 1.0;
    pos.y /= iResolution.x/iResolution.y;

    float dist1 = length(pos-sin(iTime*3.0)*0.1-0.1);
    dist1 = max(0.05/(dist1*dist1)-0.1,0.0);
    vec4 col1 = vec4(0.85,0.5,0.75,1.0);
    col1 *= (dist1+rand(pos.yx)*0.01);
    col1 = uncharted2_filmic(col1);

    float dist2 = length(pos+sin(iTime*2.0)*0.1+0.1);
    dist2 = max(0.05/(dist2*dist2)-0.1,0.0);
    vec4 col2 = vec4(0.2,0.85,0.95,1.0);
    col2 *= (dist2+rand(pos.yx)*0.01);
    col2 = uncharted2_filmic(col2);

    fragColor = col1+(1.0-col1.a)*col2;
}

// color picker
vec3 hueToRgb(float hue)
{
    const vec4 K = vec4(1,2./3.,1./3.,3);
    return clamp(abs(fract(hue+K.xyz)*6.-K.w)-K.x,0.,1.);
}

vec2 rotate2D(vec2 p, float a)
{
    float s=sin(a),c=cos(a);
    return mat2(c,s,-s,c)*p;
}

float aastep(float f)
{
    vec2 grad = vec2(dFdx(f), dFdy(f));
    float afwidth = 0.7071 * length(grad);
    return smoothstep(-afwidth, afwidth, f);
}

float polygon(vec2 st, float radius, float sides) {
    float angle = atan(st.x,st.y) + 3.1415926535;
    float slice = 6.283185307 / sides;
    return aastep(cos(floor(0.5 + angle / slice ) * slice - angle) * length(st)-radius);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    vec2 uv = fragCoord/iResolution.xy;
    vec2 pos = 2.*uv-1.;
    pos.y /= iResolution.x/iResolution.y;

    vec3 col;

    if ((int(iTime/4.0)&1)==0)
    {
        float t1 = 0.5+atan(-pos.y,-pos.x)*0.1591549430918;
        float t0 = iTime;

        col = hueToRgb(t1)*(1.0-aastep(abs(length(pos)-0.45)-0.05));
        col += (1.0-polygon(rotate2D(pos,t0),0.2,3.0))*hueToRgb(fract(-t0/6.283185307)+0.25);
    }
    else
    {
        float t1 = uv.x, t2 = 1.-uv.y;
        float t0 = sin(iTime)*0.5+0.5;

        col = t2<.9375
        ? mix(mix(vec3(1),hueToRgb(t0),t1),vec3(0),t2/.9375)
        : hueToRgb(t1)+step(abs(t1-t0),2./iResolution.x);
    }

    fragColor = vec4(col,1);
}

// rrect
float sdRBox(vec2 p, vec2 b, vec4 r)
{
    r.xy = (p.x>0.0)?r.xy : r.zw;
    r.x  = (p.y>0.0)?r.x  : r.y;
    p = abs(p)-b+r.x;
    return length(max(p,0.0)) + min(max(p.x,p.y),0.0)-r.x;
}

float aastep(float f)
{
    vec2 grad = vec2(dFdx(f), dFdy(f));
    float afwidth = 0.7071 * length(grad);
    return smoothstep(-afwidth, afwidth, f);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 uv = fragCoord/iResolution.xy;
    vec2 pos = 2.0*uv-1.0;
    pos.y *= iResolution.y/iResolution.x;

    float dis = sdRBox(pos,vec2(0.2),vec4(0.2,0.0,0.2,0.2));
    vec3 col = vec3(1.0,0.9,1.0);
    col *= 1.0 - 0.5 * exp(-iResolution.x * 0.05 * abs(dis));
    col = mix( col, vec3(239,202,195)/255., 1.0-(aastep(dis)));

    fragColor = vec4(col,1.0);
}

// what is this
vec2 rotate2D(vec2 p, float a)
{
    float s=sin(a),c=cos(a);
    return mat2(c,s,-s,c)*p;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 uv = fragCoord/iResolution.xy;
    vec2 pos = 2.0 * uv - 1.0;
    pos.y /= iResolution.x/iResolution.y;

    pos=rotate2D(pos,iTime);

    vec3 col;
    for (int j=0;j<20;j++) {
        float t = -iTime*2.0-float(j)/20.0*6.2831852;
        float s = sin(t);
        float c = cos(t);

        vec2 dir = vec2(s,c)*(0.4+0.03*sin(6.0*atan(pos.y,pos.x)));

        float dist = distance(pos,dir);
        dist = pow(0.1/max(dist,0.0001),1.3);
        vec3 co = 0.5 + 0.3*cos(iTime+pos.xyx+vec3(0,2,4));
        col += co * dist;
    }
    col = 1.0 - exp(-col*0.2);

    fragColor = vec4(col,1.0);
}

vec3 rgb2hsv(vec3 c)
{
    const vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

// glowing helix

const float DOTS = 30.0;
const vec3 COLOR = vec3(0.3, 0.6, 1.0);

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    vec2 p = (fragCoord.xy * 2.0 - iResolution.xy) / min(iResolution.x, iResolution.y);

    float f = 0.0;

    for(float i = 1.0; i <= DOTS; i++)
    {
        float t=i/3.0,
        r=sqrt(t)/8.0,
        s=sin(t),
        c=cos(t);
        f += 0.01 / abs(distance(p*0.5,vec2(s,c)*r));
    }

    vec3 col = COLOR*f;
    col = 1.0 - exp(-col);

    fragColor = vec4(col, 1.0);
}
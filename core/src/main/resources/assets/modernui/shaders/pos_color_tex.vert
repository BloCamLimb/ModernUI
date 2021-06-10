#version 450 core

layout(std140, binding = 0) uniform MatrixBlock {
    mat4 u_MVP[128];
};

layout(location = 0) in vec2 a_Pos;
layout(location = 1) in vec4 a_Color;
layout(location = 2) in vec2 a_UV;

smooth out vec2 f_Position;
smooth out vec4 f_Color;
smooth out vec2 f_TexCoord;

void main() {
    f_Position = a_Pos;
    f_Color = a_Color;
    f_TexCoord = a_UV;

    gl_Position = u_MVP[gl_InstanceID] * vec4(a_Pos, 0.0, 1.0);
}
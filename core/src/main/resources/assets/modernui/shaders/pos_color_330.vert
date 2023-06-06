#version 330 core

layout(std140) uniform MatrixBlock {
    mat4 u_Projection;
    mat4 u_ModelView;
    vec4 u_Color;
};

layout(location = 0) in vec2 a_Pos;
layout(location = 1) in vec4 a_Color;
//layout(location = 2) in mat4 a_ModelView;

out vec2 f_Position;
out vec4 f_Color;

void main() {
    f_Position = a_Pos;
    f_Color = a_Color;

    gl_Position = u_Projection * u_ModelView * vec4(a_Pos, 0.0, 1.0);
}
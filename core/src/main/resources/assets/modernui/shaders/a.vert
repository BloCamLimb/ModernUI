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
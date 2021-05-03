#version 430 compatibility

smooth out vec2 f_Position;
smooth out vec4 f_Color;

void main() {
    f_Position = gl_Vertex.xy;
    f_Color = gl_Color;

    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
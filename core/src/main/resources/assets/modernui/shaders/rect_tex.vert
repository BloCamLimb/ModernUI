#version 430 compatibility

smooth out vec2 f_Position;
smooth out vec4 f_Tint;
smooth out vec2 f_TexCoord;

void main() {
    f_Position = gl_Vertex.xy;
    f_Tint = gl_Color;

    f_TexCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
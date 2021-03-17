#version 430 compatibility

out vec2 f_Position;
out vec4 f_Tint;
out vec2 f_TexCoord;

void main() {
    f_Position = gl_Vertex.xy;
    f_Tint = gl_Color;

    f_TexCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
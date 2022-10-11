#version 450 core

layout(location = 1) smooth in vec4 f_Color;

layout(location = 0, index = 0) out vec4 fragColor;

void main() {
    fragColor = f_Color;
}
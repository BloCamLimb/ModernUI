#version 450 core

precision mediump float;

smooth in vec4 f_Color;

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = f_Color;
}
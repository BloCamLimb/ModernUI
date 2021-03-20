#version 430 core

precision highp float;

smooth in vec4 f_Color;

out vec4 fragColor;

void main() {
    fragColor = f_Color;
}
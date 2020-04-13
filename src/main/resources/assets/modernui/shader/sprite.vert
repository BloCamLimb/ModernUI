#version 430

uniform mat4 u_MVPMatrix;
uniform float u_alpha;

layout(location = 0) in vec3 position;
layout(location = 1) in vec4 color;

out vec4 v_color;

void main() {
    v_color = color * vec4(1.0, 1.0, 1.0, u_alpha);
    gl_Position = u_MVPMatrix * vec4(position, 1.0);
}
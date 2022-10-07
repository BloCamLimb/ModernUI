#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    // add bias to sharpen texture
    vec4 color = texture(Sampler0, texCoord0, -0.225) * vertexColor;
    if (color.a < 0.01) discard;
    fragColor = color * ColorModulator;
}

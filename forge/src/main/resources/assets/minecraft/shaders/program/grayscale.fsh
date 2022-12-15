#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

void main() {
    vec4 col = texture(DiffuseSampler, texCoord);
    fragColor = vec4(vec3(dot(col.rgb,vec3(0.2126,0.7152,0.0722))), col.a);
}
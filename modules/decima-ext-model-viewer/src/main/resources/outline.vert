#version 330 core

in vec2 in_position;
in vec2 in_uv;

out vec2 FragUV;

void main() {
    gl_Position = vec4(in_position, 0.0, 1.0);
    FragUV = in_uv;
}
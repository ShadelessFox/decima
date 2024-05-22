#version 330 core

in vec3 in_position;
in vec3 in_color;

uniform mat4 view;
uniform mat4 projection;

out vec3 fragmentColor;

void main() {
    gl_Position = projection * view * vec4(in_position, 1.0);
    fragmentColor = in_color;
}
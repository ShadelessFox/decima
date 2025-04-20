#version 330 core

in vec3 fragmentColor;

out vec4 FragColor;
out vec4 FragMask;

void main() {
    FragColor = vec4(fragmentColor, 1.0);
    FragMask = vec4(0.0);
}
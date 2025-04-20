#version 330 core

in vec4 fragmentColor;

out vec4 FragColor;
out vec4 FragMask;

void main() {
    FragColor = fragmentColor;
    FragMask = vec4(0.0);
}
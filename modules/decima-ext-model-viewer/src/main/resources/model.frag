#version 330 core

uniform vec3 viewPos;
uniform vec3 baseColor;

in vec3 fragmentPos;
in vec3 fragmentNormal;
in vec4 fragmentBlendIndices;
in vec4 fragmentBlendWeights;

out vec4 FragColor;

void main() {
    vec3 norm = normalize(fragmentNormal);
    vec3 viewDir = normalize(viewPos - fragmentPos);
    vec3 result = max(dot(viewDir, norm), 0.2) * baseColor;

    FragColor = vec4(result, 1.0);
}
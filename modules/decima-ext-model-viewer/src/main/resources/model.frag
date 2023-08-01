#version 330 core

const int FLAG_SOFT_SHADING = 1;
const int FLAG_WIREFRAME    = 1 << 1;

uniform vec3 viewPos;
uniform vec3 baseColor;
uniform int flags;

in vec3 fragmentPos;
in vec3 fragmentNormal;
in vec4 fragmentBlendIndices;
in vec4 fragmentBlendWeights;

out vec4 FragColor;

void main() {
    vec3 normal;

    if ((flags & FLAG_SOFT_SHADING) != 0) {
        normal = normalize(fragmentNormal);
    } else {
        normal = normalize(cross(dFdx(fragmentPos), dFdy(fragmentPos)));
    }

    vec3 color;

    if ((flags & FLAG_WIREFRAME) != 0) {
        color = vec3(1.0);
    } else {
        vec3 view = normalize(viewPos - fragmentPos);
        color = max(dot(view, normal), 0.2) * baseColor;
    }

    FragColor = vec4(color, 1.0);
}
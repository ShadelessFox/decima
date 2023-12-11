#version 330 core

const int FLAG_SOFT_SHADED  = 1;
const int FLAG_WIREFRAME    = 1 << 1;
const int FLAG_SELECTED     = 1 << 2;

uniform vec3 viewPos;
uniform vec3 baseColor;
uniform int flags;

in vec3 fragmentPos;
in vec3 fragmentNormal;
in vec4 fragmentBlendIndices;
in vec4 fragmentBlendWeights;

out vec4 FragColor;
out vec4 FragMask;

void main() {
    vec3 normal;

    if ((flags & FLAG_SOFT_SHADED) != 0) {
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
    FragMask = vec4(baseColor, float(flags & FLAG_SELECTED));
}
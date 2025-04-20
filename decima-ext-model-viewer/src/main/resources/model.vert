#version 330 core

in vec3 in_position;
in vec3 in_normal;
in vec4 in_blend_indices;
in vec4 in_blend_weights;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

out vec3 fragmentPos;
out vec3 fragmentNormal;
out vec4 fragmentBlendIndices;
out vec4 fragmentBlendWeights;

void main() {
    gl_Position = projection * view * model * vec4(in_position, 1.0);

    fragmentPos = vec3(model * vec4(in_position, 1.0));
    fragmentNormal = mat3(transpose(inverse(model))) * in_normal;
    fragmentBlendIndices = in_blend_indices;
    fragmentBlendWeights = in_blend_weights;
}
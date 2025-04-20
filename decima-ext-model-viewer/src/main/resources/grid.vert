// https://asliceofrendering.com/scene%20helper/2020/01/05/InfiniteGrid/

#version 330 core

in vec2 in_position;

uniform mat4 view;
uniform mat4 projection;

out vec3 nearPoint;
out vec3 farPoint;

vec3 unprojectPoint(float x, float y, float z) {
    mat4 viewInv = inverse(view);
    mat4 projInv = inverse(projection);
    vec4 unprojectedPoint = viewInv * projInv * vec4(x, y, z, 1.0);
    return unprojectedPoint.xyz / unprojectedPoint.w;
}

void main() {
    nearPoint = unprojectPoint(in_position.x, in_position.y, 0.0).xyz;
    farPoint = unprojectPoint(in_position.x, in_position.y, 1.0).xyz;
    gl_Position = vec4(in_position, 0.0, 1.0);
}
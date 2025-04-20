#version 330 core

in vec3 in_position;
in vec3 in_normal;

out VS_OUT {
    vec3 normal;
} vs_out;

uniform mat4 model;
uniform mat4 view;

void main() {
    gl_Position = view * model * vec4(in_position, 1.0); 
    mat3 normalMatrix = mat3(transpose(inverse(view * model)));
    vs_out.normal = normalize(normalMatrix * in_normal);
}
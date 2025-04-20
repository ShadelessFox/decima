#version 330 core

layout (triangles) in;
layout (line_strip, max_vertices = 8) out;

in VS_OUT {
    vec3 normal;
} gs_in[];

uniform mat4 projection;

out vec4 fragmentColor;

void GenerateVertexLine(int index) {
    gl_Position = projection * gl_in[index].gl_Position;
    EmitVertex();

    gl_Position = projection * (gl_in[index].gl_Position + vec4(gs_in[index].normal, 0.0) * 0.005);
    EmitVertex();

    EndPrimitive();
}

void GenerateTriangleLine() {
    vec4 position = (gl_in[0].gl_Position + gl_in[1].gl_Position + gl_in[2].gl_Position) / 3.0;
    vec4 normal = vec4((gs_in[0].normal + gs_in[1].normal + gs_in[2].normal) / 3.0, 0.0);

    gl_Position = projection * position;
    EmitVertex();

    gl_Position = projection * (position + normal * 0.01);
    EmitVertex();

    EndPrimitive();
}

void main() {
    fragmentColor = vec4(1.0, 1.0, 0.0, 1.0);
    GenerateVertexLine(0);
    GenerateVertexLine(1);
    GenerateVertexLine(2);

    fragmentColor = vec4(1.0, 0.0, 0.0, 1.0);
    GenerateTriangleLine();
}
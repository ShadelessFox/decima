#version 330 core

uniform sampler2D SamplerDiffuse;
uniform sampler2D SamplerDepth;
uniform vec2 size;

in vec2 FragUV;

out vec4 FragColor;

void make_kernel(inout vec4 n[9], sampler2D tex, vec2 coord) {
    float w = 1.0 / size.x;
    float h = 1.0 / size.y;

    n[0] = texture2D(tex, coord + vec2(-w, -h));
    n[1] = texture2D(tex, coord + vec2(0.0, -h));
    n[2] = texture2D(tex, coord + vec2(w, -h));
    n[3] = texture2D(tex, coord + vec2(-w, 0.0));
    n[4] = texture2D(tex, coord);
    n[5] = texture2D(tex, coord + vec2(w, 0.0));
    n[6] = texture2D(tex, coord + vec2(-w, h));
    n[7] = texture2D(tex, coord + vec2(0.0, h));
    n[8] = texture2D(tex, coord + vec2(w, h));
}

void main() {
    vec4 n[9];
    make_kernel(n, SamplerDepth, FragUV);

    vec4 sobel_edge_h = n[2] + (2.0*n[5]) + n[8] - (n[0] + (2.0*n[3]) + n[6]);
    vec4 sobel_edge_v = n[0] + (2.0*n[1]) + n[2] - (n[6] + (2.0*n[7]) + n[8]);
    vec4 sobel = sqrt((sobel_edge_h * sobel_edge_h) + (sobel_edge_v * sobel_edge_v));

    FragColor = vec4(texture(SamplerDiffuse, FragUV).rgb * step(0.5, sobel.rgb), 1.0);
}
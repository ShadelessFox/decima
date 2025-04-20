// https://github.com/kiwipxl/GLSL-shaders/blob/master/outline.glsl
#version 330 core

uniform sampler2D ColorSampler;
uniform sampler2D MaskSampler;

uniform vec3 outline_color = vec3(1.0, 0.7, 0.2);
uniform float outline_thickness = 0.5;
uniform float outline_threshold = 0.5;

in vec2 FragUV;

out vec4 FragColor;

void main() {
    FragColor = texture(ColorSampler, FragUV);

    if (texture(MaskSampler, FragUV).a <= outline_threshold) {
        ivec2 size = textureSize(MaskSampler, 0);
        float uv_x = FragUV.x * size.x;
        float sum = 0.0;

        for (int n = 0; n < 9; ++n) {
            float uv_y = (FragUV.y * size.y) + (outline_thickness * float(n - 4.5));
            float h_sum = 0.0;

            h_sum += texelFetch(MaskSampler, ivec2(uv_x - (4.0 * outline_thickness), uv_y), 0).a;
            h_sum += texelFetch(MaskSampler, ivec2(uv_x - (3.0 * outline_thickness), uv_y), 0).a;
            h_sum += texelFetch(MaskSampler, ivec2(uv_x - (2.0 * outline_thickness), uv_y), 0).a;
            h_sum += texelFetch(MaskSampler, ivec2(uv_x - (1.0 * outline_thickness), uv_y), 0).a;
            h_sum += texelFetch(MaskSampler, ivec2(uv_x,                             uv_y), 0).a;
            h_sum += texelFetch(MaskSampler, ivec2(uv_x + (1.0 * outline_thickness), uv_y), 0).a;
            h_sum += texelFetch(MaskSampler, ivec2(uv_x + (2.0 * outline_thickness), uv_y), 0).a;
            h_sum += texelFetch(MaskSampler, ivec2(uv_x + (3.0 * outline_thickness), uv_y), 0).a;
            h_sum += texelFetch(MaskSampler, ivec2(uv_x + (4.0 * outline_thickness), uv_y), 0).a;

            sum += h_sum / 9.0;
        }

        if (sum / 9.0 >= 0.0001) {
            FragColor = vec4(outline_color, 1);
        }
    }
}
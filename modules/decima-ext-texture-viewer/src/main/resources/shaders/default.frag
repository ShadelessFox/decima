#version 330 core

const float GRID_TILE_SIZE = 8;
const vec3 GRID_ODD_COLOR = vec3(1.0);
const vec3 GRID_EVEN_COLOR = vec3(0.85);

uniform sampler2D u_sampler;
uniform vec2 u_viewport;
uniform vec2 u_mouse;

in vec2 FragPos;
in vec2 FragUV;

out vec4 OutColor;

float grid(vec2 st, float res) {
    vec2 grid = fract(st * res);
    return step(res, grid.x) * step(res, grid.y);
}

void main() {
    vec2 aspect = vec2(u_viewport.x / u_viewport.y, 1.0);

    vec2 uv = FragUV;
    uv -= u_mouse / u_viewport; // follow mouse
    uv *= aspect;               // fix aspect ratio

    vec2 pos = FragPos;
    pos -= u_mouse / u_viewport;
    pos *= aspect;

    // float zoom = 0.005;
    // vec2 zoom_origin = vec2(0.0);
    // uv = (uv * aspect - zoom_origin) * zoom + zoom_origin; // zoom

    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        vec2 gridPos = FragPos;
        gridPos = floor(u_viewport * gridPos / GRID_TILE_SIZE / 2);

        vec3 mask = vec3(mod(gridPos.x + mod(gridPos.y, 2.0), 2.0));
        mask = mix(GRID_ODD_COLOR, GRID_EVEN_COLOR, mask);

        OutColor = vec4(mask, 1.0);
    } else {
        OutColor = texture2D(u_sampler, uv);
    }

    if (true) {
        OutColor.xyz += step(1.0 - 1.0 / 100.0, fract(pos.x * u_viewport.x)) * 0.5;
//        OutColor.xyz += step(1.0 - 1.0 / 100.0, fract(pos.y / 100.0)) * 0.5;

//        vec2 grid = abs(fract(uv * u_viewport - 0.5) - 0.5);
//        float line = min(grid.x, grid.y);
//        vec4 gridColor = vec4(vec3(1.0, 0.0, 0.0), 1.0 - min(line, 1.0));

//        OutColor *= 0.01;
//        OutColor += gridColor;
    }
}
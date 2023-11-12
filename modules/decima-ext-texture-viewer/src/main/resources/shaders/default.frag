#version 330 core

const float GRID_TILE_SIZE = 8;
const vec3 GRID_ODD_COLOR = vec3(1.0);
const vec3 GRID_EVEN_COLOR = vec3(0.85);

const int FLAG_PIXEL_GRID = 1;
const int FLAG_HDR        = 1 << 1;

uniform sampler2D u_sampler;
uniform vec2 u_viewport;
uniform vec2 u_size;
uniform vec2 u_mouse;
uniform int u_flags;

in vec2 FragPos;
in vec2 FragUV;

out vec4 OutColor;

float grid(vec2 fragCoord, float space, float size) {
    vec2 p = fragCoord - vec2(0.5);
    vec2 a1 = mod(p - size, space);
    vec2 a2 = mod(p + size, space);
    vec2 a = a2 - a1;

    float g = min(a.x, a.y);
    return clamp(g, 0.0, 1.0);
}

void main() {
    // inputs
    vec2 origin = vec2(0.5);
    float zoom = 1.0 / 1.0;
    float gamma = 1.0, exposure = 1.0 / 16.0;

    vec2 aspect = vec2(u_viewport.x / u_viewport.y, 1.0);

    vec2 uv = FragUV;
    uv = uv - u_mouse / u_viewport;
    uv = uv * aspect * zoom;

    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        vec2 gridPos = FragPos;
        gridPos = floor(u_viewport * gridPos / GRID_TILE_SIZE / 2);

        vec3 mask = vec3(mod(gridPos.x + mod(gridPos.y, 2.0), 2.0));
        mask = mix(GRID_ODD_COLOR, GRID_EVEN_COLOR, mask);

        OutColor = vec4(mask, 1.0);
    } else {
        OutColor = texture2D(u_sampler, uv);

        if ((u_flags & FLAG_HDR) > 0) {
            OutColor.rgb = vec3(1.0) - exp(-OutColor.rgb * exposure);
            OutColor.rgb = pow(OutColor.rgb, vec3(1.0 / gamma));
        }

        if ((u_flags & FLAG_PIXEL_GRID) > 0) {
            OutColor += 1.0 - grid(FragUV * u_viewport - u_mouse, u_viewport.y / u_size.y / zoom, 0.5);
        }
    }
}
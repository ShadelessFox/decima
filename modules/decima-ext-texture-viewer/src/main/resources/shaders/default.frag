#version 330 core

const float GRID_TILE_SIZE = 8;
const vec3 GRID_ODD_COLOR = vec3(1.0);
const vec3 GRID_EVEN_COLOR = vec3(0.85);

const int FLAG_SHOW_GRID  = 1 << 0;
const int FLAG_R          = 1 << 1;
const int FLAG_G          = 1 << 2;
const int FLAG_B          = 1 << 3;
const int FLAG_A          = 1 << 4;
const int FLAG_RGB        = FLAG_R | FLAG_G | FLAG_B;
const int FLAG_RGBA       = FLAG_R | FLAG_G | FLAG_B | FLAG_A;

uniform sampler2D u_sampler;
uniform vec2 u_viewport;
uniform vec2 u_size;
uniform vec2 u_location;
uniform float u_zoom;
uniform float u_gamma;
uniform float u_exposure;
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
    vec2 viewport_aspect = vec2(u_viewport.x / u_viewport.y, 1.0);
    vec2 sampler_aspect = vec2(u_size.x / u_size.y, 1.0);
    vec2 aspect = viewport_aspect / sampler_aspect;

    vec2 uv = FragUV;
    uv = uv - u_location / u_viewport;
    uv = uv * aspect * u_zoom;

    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        vec2 gridPos = FragPos;
        gridPos = floor(u_viewport * gridPos / GRID_TILE_SIZE / 2);

        vec3 mask = vec3(mod(gridPos.x + mod(gridPos.y, 2.0), 2.0));
        mask = mix(GRID_ODD_COLOR, GRID_EVEN_COLOR, mask);

        OutColor = vec4(mask, 1.0);
    } else {
        OutColor = texture2D(u_sampler, uv);

        switch (u_flags & FLAG_RGBA) {
            case FLAG_R:
                OutColor.rgb = vec3(OutColor.r);
                break;
            case FLAG_G:
                OutColor.rgb = vec3(OutColor.g);
                break;
            case FLAG_B:
                OutColor.rgb = vec3(OutColor.b);
                break;
            case FLAG_RGB:
                OutColor.a = 1.0;
            case FLAG_RGBA:
                break;
            default:
                OutColor.r *= float(bool(u_flags & FLAG_R));
                OutColor.g *= float(bool(u_flags & FLAG_G));
                OutColor.b *= float(bool(u_flags & FLAG_B));
                OutColor.a *= float(bool(u_flags & FLAG_A));
                break;
        }

        if (u_exposure != 1.0) {
            OutColor.rgb = vec3(1.0) - exp(-OutColor.rgb * u_exposure);
        }

        if (u_gamma != 1.0) {
            OutColor.rgb = pow(OutColor.rgb, vec3(1.0 / u_gamma));
        }

        if (bool(u_flags & FLAG_SHOW_GRID)) {
            OutColor += mix(0.5, 0.0, grid(FragUV * u_viewport - u_location, u_viewport.y / u_size.y / u_zoom, 0.5));
        }
    }
}
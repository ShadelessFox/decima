// https://learnopengl.com/Advanced-OpenGL/Depth-testing
// https://asliceofrendering.com/scene%20helper/2020/01/05/InfiniteGrid/
// https://github.com/ValveResourceFormat/ValveResourceFormat/blob/master/GUI/Types/Renderer/Shaders/grid.frag

#version 330 core

uniform mat4 view;
uniform mat4 projection;

in vec3 nearPoint;
in vec3 farPoint;

out vec4 FragColor;
out vec4 FragMask;

float computeDepth(vec4 clipSpacePos) {
    float clipSpaceDepth = clipSpacePos.z / clipSpacePos.w;
    float far = gl_DepthRange.far;
    float near = gl_DepthRange.near;
    float depth = ((far - near) * clipSpaceDepth + near + far) / 2.0;
    return depth;
}

float computeFade(vec4 clipSpacePos) {
    float clipSpaceDepth = (clipSpacePos.z / clipSpacePos.w) * 2.0 - 1.0;
    float near = 0.01;
    float far = 100;
    float linearDepth = (2.0 * near * far) / (far + near - clipSpaceDepth * (far - near));
    return linearDepth / far;
}

vec4 computeGrid(vec3 point, float scale, bool isAxis) {
    vec2 coord = point.xz * scale;
    vec2 deriative = fwidth(coord);
    vec2 grid = abs(fract(coord - 0.5) - 0.5) / deriative;
    float line = min(grid.x, grid.y);
    float min_z = min(deriative.y, 1.0);
    float min_x = min(deriative.x, 1.0);

    vec4 gridColor = vec4(vec3(0.2), 1.0 - min(line, 1.0));

    if (-min_x < point.x && point.x < 0.1 * min_x && isAxis) {
        gridColor.rgb = vec3(0.2, 0.8, 0.2);
    }

    if (-min_z < point.z && point.z < 0.1 * min_z && isAxis) {
        gridColor.rgb = vec3(0.9, 0.2, 0.2);
    }

    return gridColor;
}

void main() {
    float t = -nearPoint.y / (farPoint.y - nearPoint.y);
    vec3 fragPos = nearPoint + t * (farPoint - nearPoint);
    vec4 clipSpacePos = projection * view * vec4(fragPos, 1.0);

    FragColor = computeGrid(fragPos, 1, true) + computeGrid(fragPos, 10, false) * 0.25;
    FragColor *= max(0, 1.0 - computeFade(clipSpacePos));
    FragColor *= float(t > 0);
    FragMask = vec4(0.0);

    gl_FragDepth = computeDepth(clipSpacePos);
}
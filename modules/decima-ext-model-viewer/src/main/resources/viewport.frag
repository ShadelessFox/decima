// https://learnopengl.com/Advanced-OpenGL/Depth-testing
// https://asliceofrendering.com/scene%20helper/2020/01/05/InfiniteGrid/
// https://github.com/ValveResourceFormat/ValveResourceFormat/blob/master/GUI/Types/Renderer/Shaders/grid.frag

#version 330 core

uniform mat4 view;
uniform mat4 projection;
uniform vec3 viewPos;
uniform vec3 oddColor;
uniform vec3 evenColor;

in vec3 nearPoint;
in vec3 farPoint;

out vec4 outputColor;

float near = 0.01;
float far = 100;
vec3 colorRed = vec3(0.9, 0.2, 0.2);
vec3 colorGreen = vec3(0.2, 0.8, 0.2);
vec3 colorGrid = vec3(0.9, 0.9, 1.0);

float computeLinearDepth(vec4 pos) {
    float depth = (pos.z / pos.w) * 2.0 - 1.0;
    float z = depth * 2.0 - 1.0;
    float linearDepth = (2.0 * near * far) / (far + near - z * (far - near));
    return linearDepth / far;
}

vec4 computeGridColor() {
    float t = -nearPoint.y / (farPoint.y - nearPoint.y);
    vec3 fragPos = nearPoint + t * (farPoint - nearPoint);
    vec4 clipSpacePos = projection * view * vec4(fragPos, 1.0);

    vec2 coord = fragPos.xz;
    vec2 derivative = fwidth(fragPos.xz);

    vec2 grid = abs(fract(coord - 0.5) - 0.5) / derivative.xy;
    float line = min(grid.x, grid.y);
    vec4 gridColor = vec4(colorGrid, 1.0 - min(line, 1.0));

    float linearDepth = computeLinearDepth(clipSpacePos);
    float fading = max(0, (0.5 - linearDepth));

    float angleFade = min(1.0, pow(abs(normalize(fragPos - viewPos).y), 1.4) * 100);
    gridColor.xyz *= fading * angleFade;

    vec2 axisLines = abs(coord) / derivative;

    if (axisLines.x < 1) {
        float axisLineAlpha = (1 - min(axisLines.x, 1.0));
        gridColor.a = 1 - (1 - axisLineAlpha) * (min(grid.y, 1.0));
        gridColor.xyz = gridColor.xyz * (1 - axisLineAlpha) * (1 - min(grid.y, 1.0)) + colorGreen * axisLineAlpha;
        gridColor.xyz /= gridColor.a;
        gridColor.a *= 2 - (1 - min(grid.y, 1.0)) / (2 - axisLines.x - min(grid.y, 1.0));
    }

    if (axisLines.y < 1) {
        float axisLineAlpha = (1 - min(axisLines.y, 1.0));
        float crossAxisLineAlpha = 1 - min(grid.x, 1.0);

        if (min(axisLines.x, 1.0) == 1) {
            gridColor.a = 1 - (1 - axisLineAlpha) * (1 - crossAxisLineAlpha);
            gridColor.xyz = gridColor.xyz * (1 - axisLineAlpha) * crossAxisLineAlpha + colorRed * axisLineAlpha;
            gridColor.xyz /= gridColor.a;
            gridColor.a *= mix(2, 1, crossAxisLineAlpha / (axisLineAlpha + crossAxisLineAlpha));
        } else {
            gridColor.xyz = mix(colorGreen, colorRed, axisLineAlpha / (axisLineAlpha + crossAxisLineAlpha));
            gridColor.a = max(axisLineAlpha, crossAxisLineAlpha) * 2;
        }
    }

    gridColor.a *= fading * angleFade;
    gridColor *= float(t > 0);

    return gridColor;
}

vec3 computeCheckerColor() {
    vec2 checkPos = floor(gl_FragCoord.xy / 8);
    float checkMask = mod(checkPos.x + mod(1.0 - checkPos.y, 2.0), 2.0);
    return mix(oddColor, evenColor, checkMask);
}

void main() {
    vec4 gridColor = computeGridColor();
    vec3 checkColor = computeCheckerColor();
    outputColor = vec4(checkColor * (1.0 - gridColor.a) + gridColor.rgb * gridColor.a, 1.0);
}
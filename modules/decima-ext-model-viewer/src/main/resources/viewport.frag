#version 330 core

uniform vec3 oddColor;
uniform vec3 evenColor;

out vec4 FragColor;

void main() {
    vec2 checkPos = floor(gl_FragCoord.xy / 8);
    float checkMask = mod(checkPos.x + mod(1 - checkPos.y, 2.0), 2.0);
    vec3 checkColor = mix(oddColor, evenColor, checkMask);

    FragColor = vec4(checkColor, 1.0f);
}
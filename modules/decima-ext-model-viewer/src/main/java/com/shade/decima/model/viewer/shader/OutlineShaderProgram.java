package com.shade.decima.model.viewer.shader;

import com.shade.gl.Attribute;
import com.shade.gl.Shader;
import com.shade.gl.ShaderProgram;
import com.shade.gl.UniformInt;

import java.io.IOException;
import java.util.Map;

public class OutlineShaderProgram extends ShaderProgram {
    public final UniformInt colorSampler;
    public final UniformInt maskSampler;

    public OutlineShaderProgram() throws IOException {
        super(
            Map.of(
                Shader.fromResource("outline.vert"), Shader.Type.VERTEX,
                Shader.fromResource("outline.frag"), Shader.Type.FRAGMENT
            ),
            Map.of(
                "in_position", Attribute.Semantic.POSITION,
                "in_uv", Attribute.Semantic.TEXTURE
            )
        );

        colorSampler = UniformInt.create(this, "ColorSampler");
        maskSampler = UniformInt.create(this, "MaskSampler");
    }
}

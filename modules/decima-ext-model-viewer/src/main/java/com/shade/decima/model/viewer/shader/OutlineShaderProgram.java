package com.shade.decima.model.viewer.shader;

import com.shade.gl.Attribute;
import com.shade.gl.Shader;
import com.shade.gl.ShaderProgram;
import com.shade.gl.UniformVec2;
import com.shade.util.NotNull;

import java.io.IOException;
import java.util.Map;

public class OutlineShaderProgram extends ShaderProgram {
    private final UniformVec2 size;

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

        this.size = UniformVec2.create(this, "size");
    }

    @NotNull
    public UniformVec2 getSize() {
        return size;
    }
}

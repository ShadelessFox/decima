package com.shade.decima.model.viewer.shader;

import com.shade.decima.model.viewer.gl.Shader;
import com.shade.decima.model.viewer.gl.Shader.Type;
import com.shade.decima.model.viewer.gl.ShaderProgram;
import com.shade.decima.model.viewer.gl.UniformVec3;
import com.shade.decima.model.viewer.isr.Primitive.Semantic;
import com.shade.util.NotNull;

import java.io.IOException;
import java.util.Map;

public class ViewportShaderProgram extends ShaderProgram {
    private final UniformVec3 oddColor;
    private final UniformVec3 evenColor;

    public ViewportShaderProgram() throws IOException {
        super(
            Map.of(
                Shader.fromResource("viewport.vert"), Type.VERTEX,
                Shader.fromResource("viewport.frag"), Type.FRAGMENT
            ),
            Map.of(
                "in_position", Semantic.POSITION
            )
        );

        this.oddColor = UniformVec3.create(this, "oddColor");
        this.evenColor = UniformVec3.create(this, "evenColor");
    }

    @NotNull
    public UniformVec3 getOddColor() {
        return oddColor;
    }

    @NotNull
    public UniformVec3 getEvenColor() {
        return evenColor;
    }
}

package com.shade.decima.model.viewer.shader;

import com.shade.gl.Attribute.Semantic;
import com.shade.gl.Shader;
import com.shade.gl.Shader.Type;
import com.shade.gl.ShaderProgram;
import com.shade.gl.UniformMat4;
import com.shade.util.NotNull;

import java.io.IOException;
import java.util.Map;

public class DebugShaderProgram extends ShaderProgram {
    private final UniformMat4 view;
    private final UniformMat4 projection;

    public DebugShaderProgram() throws IOException {
        super(
            Map.of(
                Shader.fromResource("debug.vert"), Type.VERTEX,
                Shader.fromResource("debug.frag"), Type.FRAGMENT
            ),
            Map.of(
                "in_position", Semantic.POSITION,
                "in_color", Semantic.COLOR
            )
        );

        this.view = UniformMat4.create(this, "view");
        this.projection = UniformMat4.create(this, "projection");
    }

    @NotNull
    @Override
    public DebugShaderProgram bind() {
        return (DebugShaderProgram) super.bind();
    }

    @NotNull
    public UniformMat4 getView() {
        return view;
    }

    @NotNull
    public UniformMat4 getProjection() {
        return projection;
    }
}

package com.shade.decima.model.viewer.shader;

import com.shade.gl.Attribute;
import com.shade.gl.Shader;
import com.shade.gl.Shader.Type;
import com.shade.gl.ShaderProgram;
import com.shade.gl.UniformMat4;
import com.shade.util.NotNull;

import java.io.IOException;
import java.util.Map;

public class GridShaderProgram extends ShaderProgram {
    private final UniformMat4 view;
    private final UniformMat4 projection;

    public GridShaderProgram() throws IOException {
        super(
            Map.of(
                Shader.fromResource("grid.vert"), Type.VERTEX,
                Shader.fromResource("grid.frag"), Type.FRAGMENT
            ),
            Map.of(
                "in_position", Attribute.Semantic.POSITION
            )
        );

        this.view = UniformMat4.create(this, "view");
        this.projection = UniformMat4.create(this, "projection");
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

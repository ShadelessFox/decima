package com.shade.decima.model.viewer.shader;

import com.shade.decima.model.viewer.gl.Shader;
import com.shade.decima.model.viewer.gl.ShaderProgram;
import com.shade.decima.model.viewer.gl.UniformMat4;
import com.shade.util.NotNull;

import java.io.IOException;

public class NormalShaderProgram extends ShaderProgram {
    private final UniformMat4 model;
    private final UniformMat4 view;
    private final UniformMat4 projection;

    public NormalShaderProgram() throws IOException {
        super(
            Shader.fromResource(Shader.Type.VERTEX, "normal.vert"),
            Shader.fromResource(Shader.Type.FRAGMENT, "normal.frag"),
            Shader.fromResource(Shader.Type.GEOMETRY, "normal.geom"),
            "in_position", "in_normal"
        );

        this.model = UniformMat4.create(this, "model");
        this.view = UniformMat4.create(this, "view");
        this.projection = UniformMat4.create(this, "projection");
    }

    @NotNull
    public UniformMat4 getModel() {
        return model;
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

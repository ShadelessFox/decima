package com.shade.decima.model.viewer.shader;

import com.shade.decima.model.viewer.gl.Shader;
import com.shade.decima.model.viewer.gl.ShaderProgram;
import com.shade.decima.model.viewer.gl.UniformMat4;
import com.shade.util.NotNull;

public abstract class ModelShaderProgram extends ShaderProgram {
    private final UniformMat4 model;
    private final UniformMat4 view;
    private final UniformMat4 projection;

    public ModelShaderProgram(@NotNull Shader[] shaders, @NotNull String[] attributes) {
        super(shaders, attributes);

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

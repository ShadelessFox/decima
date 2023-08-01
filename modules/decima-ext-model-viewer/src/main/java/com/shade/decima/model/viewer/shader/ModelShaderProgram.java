package com.shade.decima.model.viewer.shader;

import com.shade.decima.model.viewer.gl.*;
import com.shade.util.NotNull;

import java.io.IOException;

public class ModelShaderProgram extends ShaderProgram {
    public static final int FLAG_SOFT_SHADING = 1;
    public static final int FLAG_WIREFRAME = 1 << 1;

    private final UniformMat4 model;
    private final UniformMat4 view;
    private final UniformMat4 projection;
    private final UniformVec3 position;
    private final UniformVec3 color;
    private final UniformInt flags;

    public ModelShaderProgram() throws IOException {
        super(
            Shader.fromResource(Shader.Type.VERTEX, "model.vert"),
            Shader.fromResource(Shader.Type.FRAGMENT, "model.frag"),
            "in_position", "in_normal", "in_blend_indices", "in_blend_weights"
        );

        this.model = UniformMat4.create(this, "model");
        this.view = UniformMat4.create(this, "view");
        this.projection = UniformMat4.create(this, "projection");
        this.position = UniformVec3.create(this, "viewPos");
        this.color = UniformVec3.create(this, "baseColor");
        this.flags = UniformInt.create(this, "flags");
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

    @NotNull
    public UniformVec3 getPosition() {
        return position;
    }

    @NotNull
    public UniformVec3 getColor() {
        return color;
    }

    @NotNull
    public UniformInt getFlags() {
        return flags;
    }
}

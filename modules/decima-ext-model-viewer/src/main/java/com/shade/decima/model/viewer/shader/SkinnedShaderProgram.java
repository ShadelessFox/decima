package com.shade.decima.model.viewer.shader;

import com.shade.decima.model.viewer.gl.Shader;
import com.shade.decima.model.viewer.gl.UniformInt;
import com.shade.decima.model.viewer.gl.UniformVec3;
import com.shade.util.NotNull;

import java.io.IOException;

public class SkinnedShaderProgram extends ModelShaderProgram {
    public static final int FLAG_SOFT_SHADING = 1;
    public static final int FLAG_WIREFRAME = 1 << 1;

    private final UniformVec3 position;
    private final UniformVec3 color;
    private final UniformInt flags;

    public SkinnedShaderProgram() throws IOException {
        super(
            new Shader[]{
                Shader.fromResource(Shader.Type.VERTEX, "model.vert"),
                Shader.fromResource(Shader.Type.FRAGMENT, "model.frag")
            },
            new String[]{
                "in_position",
                "in_normal",
                "in_blend_indices",
                "in_blend_weights"
            }
        );

        this.position = UniformVec3.create(this, "viewPos");
        this.color = UniformVec3.create(this, "baseColor");
        this.flags = UniformInt.create(this, "flags");
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

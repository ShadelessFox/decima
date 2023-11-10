package com.shade.decima.model.viewer.shader;

import com.shade.gl.Attribute;
import com.shade.gl.Shader;
import com.shade.gl.Shader.Type;
import com.shade.gl.UniformInt;
import com.shade.gl.UniformVec3;
import com.shade.util.NotNull;

import java.io.IOException;
import java.util.Map;

public class RegularShaderProgram extends ModelShaderProgram {
    public static final int FLAG_SOFT_SHADED = 1;
    public static final int FLAG_WIREFRAME = 1 << 1;

    private final UniformVec3 position;
    private final UniformVec3 color;
    private final UniformInt flags;

    public RegularShaderProgram() throws IOException {
        super(
            Map.of(
                Shader.fromResource("model.vert"), Type.VERTEX,
                Shader.fromResource("model.frag"), Type.FRAGMENT
            ),
            Map.of(
                "in_position", Attribute.Semantic.POSITION,
                "in_normal", Attribute.Semantic.NORMAL,
                "in_blend_indices", Attribute.Semantic.JOINTS,
                "in_blend_weights", Attribute.Semantic.WEIGHTS
            )
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

    public boolean isSoftShaded() {
        return getFlag(FLAG_SOFT_SHADED);
    }

    public void setSoftShaded(boolean softShading) {
        setFlag(FLAG_SOFT_SHADED, softShading);
    }

    public boolean isWireframe() {
        return getFlag(FLAG_WIREFRAME);
    }

    public void setWireframe(boolean wireframe) {
        setFlag(FLAG_WIREFRAME, wireframe);
    }

    public boolean getFlag(int flag) {
        return (flags.get() & flag) != 0;
    }

    public void setFlag(int flag, boolean enabled) {
        if (enabled) {
            flags.set(flags.get() | flag);
        } else {
            flags.set(flags.get() & ~flag);
        }
    }
}

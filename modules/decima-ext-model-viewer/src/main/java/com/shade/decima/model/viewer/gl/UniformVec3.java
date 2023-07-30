package com.shade.decima.model.viewer.gl;

import com.shade.util.NotNull;
import org.joml.Vector3fc;
import org.lwjgl.opengl.GL20;

public class UniformVec3 extends Uniform<Vector3fc> {
    private UniformVec3(@NotNull String name, int location) {
        super(name, location);
    }

    @NotNull
    public static UniformVec3 create(@NotNull ShaderProgram program, @NotNull String name) {
        return program.bindUniform(UniformVec3::new, name);
    }

    @Override
    public void set(@NotNull Vector3fc value) {
        GL20.glUniform3f(location, value.x(), value.y(), value.z());
    }
}

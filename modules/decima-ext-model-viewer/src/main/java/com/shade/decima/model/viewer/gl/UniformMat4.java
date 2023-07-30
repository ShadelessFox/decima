package com.shade.decima.model.viewer.gl;

import com.shade.util.NotNull;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL20;

public class UniformMat4 extends Uniform<Matrix4fc> {
    private final float[] buffer = new float[4 * 4];

    private UniformMat4(@NotNull String name, int location) {
        super(name, location);
    }

    @NotNull
    public static UniformMat4 create(@NotNull ShaderProgram program, @NotNull String name) {
        return program.bindUniform(UniformMat4::new, name);
    }

    @Override
    public void set(@NotNull Matrix4fc value) {
        GL20.glUniformMatrix4fv(location, false, value.get(buffer));
    }
}

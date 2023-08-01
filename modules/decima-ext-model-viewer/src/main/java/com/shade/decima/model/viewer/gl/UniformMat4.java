package com.shade.decima.model.viewer.gl;

import com.shade.util.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

public class UniformMat4 extends Uniform<Matrix4fc> {
    private final FloatBuffer buffer = BufferUtils.createFloatBuffer(16);

    private UniformMat4(@NotNull String name, int program, int location) {
        super(name, program, location);
    }

    @NotNull
    public static UniformMat4 create(@NotNull ShaderProgram program, @NotNull String name) {
        return program.bindUniform(UniformMat4::new, name);
    }

    @NotNull
    @Override
    public Matrix4fc get() {
        GL20.glGetUniformfv(program, location, buffer);
        return new Matrix4f(buffer);
    }

    @Override
    public void set(@NotNull Matrix4fc value) {
        GL20.glUniformMatrix4fv(location, false, value.get(buffer));
    }
}

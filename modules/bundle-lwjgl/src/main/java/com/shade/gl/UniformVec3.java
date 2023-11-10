package com.shade.gl;

import com.shade.util.NotNull;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

public class UniformVec3 extends Uniform<Vector3fc> {
    private final FloatBuffer buffer = BufferUtils.createFloatBuffer(3);

    private UniformVec3(@NotNull String name, int program, int location) {
        super(name, program, location);
    }

    @NotNull
    public static UniformVec3 create(@NotNull ShaderProgram program, @NotNull String name) {
        return program.bindUniform(UniformVec3::new, name);
    }

    @NotNull
    @Override
    public Vector3fc get() {
        GL20.glGetUniformfv(program, location, buffer);
        return new Vector3f(buffer);
    }

    @Override
    public void set(@NotNull Vector3fc value) {
        GL20.glUniform3fv(location, value.get(buffer));
    }
}

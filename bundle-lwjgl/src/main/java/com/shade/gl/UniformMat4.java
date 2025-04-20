package com.shade.gl;

import com.shade.util.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class UniformMat4 extends Uniform<Matrix4fc> {
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
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final FloatBuffer buffer = stack.mallocFloat(16);
            GL20.glGetUniformfv(program, location, buffer);
            return new Matrix4f(buffer);
        }
    }

    @Override
    public void set(@NotNull Matrix4fc value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final FloatBuffer buffer = stack.mallocFloat(16);
            GL20.glUniformMatrix4fv(location, false, value.get(buffer));
        }
    }
}

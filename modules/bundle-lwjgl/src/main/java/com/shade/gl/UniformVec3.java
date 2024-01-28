package com.shade.gl;

import com.shade.util.NotNull;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;

public class UniformVec3 extends Uniform<Vector3fc> {
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
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final FloatBuffer buffer = stack.mallocFloat(3);
            glGetUniformfv(program, location, buffer);
            return new Vector3f(buffer);
        }
    }

    @Override
    public void set(@NotNull Vector3fc value) {
        glUniform3f(location, value.x(), value.y(), value.z());
    }
}

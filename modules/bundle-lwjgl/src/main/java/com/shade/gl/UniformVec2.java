package com.shade.gl;

import com.shade.util.NotNull;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;

public class UniformVec2 extends Uniform<Vector2fc> {
    private UniformVec2(@NotNull String name, int program, int location) {
        super(name, program, location);
    }

    @NotNull
    public static UniformVec2 create(@NotNull ShaderProgram program, @NotNull String name) {
        return program.bindUniform(UniformVec2::new, name);
    }

    @NotNull
    @Override
    public Vector2fc get() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final FloatBuffer buffer = stack.mallocFloat(2);
            glGetUniformfv(program, location, buffer);
            return new Vector2f(buffer);
        }
    }

    @Override
    public void set(@NotNull Vector2fc value) {
        glUniform2f(location, value.x(), value.y());
    }
}

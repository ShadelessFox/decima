package com.shade.gl;

import com.shade.util.NotNull;
import org.lwjgl.opengl.GL20;

public class UniformInt extends Uniform<Integer> {
    private UniformInt(@NotNull String name, int program, int location) {
        super(name, program, location);
    }

    @NotNull
    public static UniformInt create(@NotNull ShaderProgram program, @NotNull String name) {
        return program.bindUniform(UniformInt::new, name);
    }

    @NotNull
    @Override
    public Integer get() {
        return GL20.glGetUniformi(program, location);
    }

    @Override
    public void set(@NotNull Integer value) {
        GL20.glUniform1i(location, value);
    }

    public void set(int value) {
        GL20.glUniform1i(location, value);
    }

    public void or(int value) {
        set(get() | value);
    }

    public void and(int value) {
        set(get() & value);
    }
}

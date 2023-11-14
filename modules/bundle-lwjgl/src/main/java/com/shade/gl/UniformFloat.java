package com.shade.gl;

import com.shade.util.NotNull;
import org.lwjgl.opengl.GL20;

public class UniformFloat extends Uniform<Float> {
    private UniformFloat(@NotNull String name, int program, int location) {
        super(name, program, location);
    }

    @NotNull
    public static UniformFloat create(@NotNull ShaderProgram program, @NotNull String name) {
        return program.bindUniform(UniformFloat::new, name);
    }

    @NotNull
    @Override
    public Float get() {
        return GL20.glGetUniformf(program, location);
    }

    @Override
    public void set(@NotNull Float value) {
        GL20.glUniform1f(location, value);
    }

    public void set(float value) {
        GL20.glUniform1f(location, value);
    }
}

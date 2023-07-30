package com.shade.decima.model.viewer.gl;

import com.shade.platform.model.Disposable;
import com.shade.util.NotNull;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram implements Disposable {
    private final int program;

    public ShaderProgram(@NotNull Shader vertexShader, @NotNull Shader fragmentShader, @NotNull String... attributes) {
        this(new Shader[]{vertexShader, fragmentShader}, attributes);
    }

    public ShaderProgram(@NotNull Shader[] shaders, @NotNull String[] attributes) {
        this.program = glCreateProgram();

        final int[] shaderIds = new int[shaders.length];

        for (int i = 0; i < shaders.length; i++) {
            final Shader shader = shaders[i];
            final int id = glCreateShader(shader.type().getGlType());

            glShaderSource(id, shader.source());
            glCompileShader(id);

            if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
                throw new IllegalStateException(shader.name() + ": " + glGetShaderInfoLog(id));
            }

            glAttachShader(program, id);

            shaderIds[i] = id;
        }

        for (int i = 0; i < attributes.length; i++) {
            glBindAttribLocation(program, i, attributes[i]);
        }

        glLinkProgram(program);

        for (int shader : shaderIds) {
            glDetachShader(program, shader);
            glDeleteShader(shader);
        }
    }

    public void bind() {
        glUseProgram(program);
    }

    public void unbind() {
        glUseProgram(0);
    }

    @Override
    public void dispose() {
        glUseProgram(0);
        glDeleteProgram(program);
    }

    @NotNull
    <T extends Uniform<?>> T bindUniform(@NotNull UniformCreator<T> supplier, @NotNull String name) {
        final int location = glGetUniformLocation(program, name);

        if (location < 0) {
            throw new IllegalArgumentException("Can't find uniform '" + name + "' in program " + program);
        }

        return supplier.create(name, location);
    }

    protected interface UniformCreator<T extends Uniform<?>> {
        @NotNull
        T create(@NotNull String name, int location);
    }
}

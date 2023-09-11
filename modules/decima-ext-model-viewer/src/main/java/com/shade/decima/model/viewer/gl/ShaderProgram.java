package com.shade.decima.model.viewer.gl;

import com.shade.decima.model.viewer.isr.Primitive;
import com.shade.platform.model.Disposable;
import com.shade.util.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram implements Disposable {
    private final int program;

    public ShaderProgram(
        @NotNull Map<Shader, Shader.Type> shaders,
        @NotNull Map<String, Primitive.Semantic> attributes
    ) {
        this.program = glCreateProgram();

        final List<Integer> shaderIds = new ArrayList<>(shaders.size());

        for (Map.Entry<Shader, Shader.Type> entry : shaders.entrySet()) {
            final Shader shader = entry.getKey();
            final int id = glCreateShader(entry.getValue().getGlType());

            glShaderSource(id, shader.source());
            glCompileShader(id);

            if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
                throw new IllegalStateException(shader.name() + ": " + glGetShaderInfoLog(id));
            }

            glAttachShader(program, id);

            shaderIds.add(id);
        }

        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Program[" + program + "]" + ": " + glGetProgramInfoLog(program));
        }

        for (int shader : shaderIds) {
            glDetachShader(program, shader);
            glDeleteShader(shader);
        }

        for (Map.Entry<String, Primitive.Semantic> entry : attributes.entrySet()) {
            glBindAttribLocation(program, entry.getValue().ordinal(), entry.getKey());
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

        return supplier.create(name, program, location);
    }

    protected interface UniformCreator<T extends Uniform<?>> {
        @NotNull
        T create(@NotNull String name, int program, int location);
    }
}

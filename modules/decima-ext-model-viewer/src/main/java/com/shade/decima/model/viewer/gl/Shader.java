package com.shade.decima.model.viewer.gl;

import com.shade.util.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;

public record Shader(@NotNull Type type, @NotNull String name, @NotNull String source) {
    public enum Type {
        VERTEX(GL_VERTEX_SHADER),
        FRAGMENT(GL_FRAGMENT_SHADER),
        GEOMETRY(GL_GEOMETRY_SHADER);

        private final int glType;

        Type(int glType) {
            this.glType = glType;
        }

        public int getGlType() {
            return glType;
        }
    }

    public Shader(@NotNull Type type, @NotNull String name, @NotNull String source) {
        this.type = type;
        this.name = name;
        this.source = source;
    }

    @NotNull
    public static Shader fromFile(@NotNull Type type, @NotNull Path path) throws IOException {
        return fromFile(type, path.toAbsolutePath().toString(), path);
    }

    @NotNull
    public static Shader fromFile(@NotNull Type type, @NotNull String name, @NotNull Path path) throws IOException {
        return new Shader(type, name, Files.readString(path));
    }

    @NotNull
    public static Shader fromStream(@NotNull Type type, @NotNull String name, @NotNull InputStream stream) throws IOException {
        return new Shader(type, name, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
    }

    @NotNull
    public static Shader fromResource(@NotNull Type type, @NotNull String name) throws IOException {
        try (InputStream stream = Shader.class.getClassLoader().getResourceAsStream(name)) {
            if (stream == null) {
                throw new IOException("Can't find resource '" + name + "'");
            }

            return fromStream(type, name, stream);
        }
    }
}

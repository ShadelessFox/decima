package com.shade.decima.app.gl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public record ShaderSource(String name, String source) {
    public static ShaderSource fromResource(String name, ClassLoader classLoader) throws IOException {
        try (InputStream stream = classLoader.getResourceAsStream(name)) {
            if (stream == null) {
                throw new IOException("Can't find resource '" + name + "'");
            }
            return fromStream(name, stream);
        }
    }

    public static ShaderSource fromStream(String name, InputStream stream) throws IOException {
        String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        return new ShaderSource(name, source);
    }
}

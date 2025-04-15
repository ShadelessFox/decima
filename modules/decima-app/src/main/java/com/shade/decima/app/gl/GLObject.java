package com.shade.decima.app.gl;

public interface GLObject extends AutoCloseable {
    GLObject bind();

    void unbind();

    void dispose();

    @Override
    default void close() {
        unbind();
    }
}

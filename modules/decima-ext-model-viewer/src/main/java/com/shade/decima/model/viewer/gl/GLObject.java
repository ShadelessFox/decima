package com.shade.decima.model.viewer.gl;

import com.shade.platform.model.Disposable;
import com.shade.util.NotNull;

public interface GLObject<T extends GLObject<?>> extends Disposable, AutoCloseable {
    @NotNull
    T bind();

    void unbind();

    @Override
    default void close() {
        unbind();
    }
}

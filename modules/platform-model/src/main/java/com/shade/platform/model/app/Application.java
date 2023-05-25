package com.shade.platform.model.app;

import com.shade.platform.model.ElementFactory;
import com.shade.util.NotNull;

public interface Application {
    void start(@NotNull String[] args);

    <T> T getService(@NotNull Class<T> cls);

    @NotNull
    ElementFactory getElementFactory(@NotNull String id);
}

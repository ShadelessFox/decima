package com.shade.platform.model.persistence;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

public interface PersistableComponent<T> {
    @Nullable
    T getState();

    void loadState(@NotNull T state);

    default void noStateLoaded() {
        // do nothing by default
    }
}

package com.shade.decima.model.app;

import com.shade.decima.model.util.NotNull;

public interface DataContext {
    DataContext EMPTY = key -> null;

    Object getData(@NotNull String key);

    default <T> T getData(@NotNull DataKey<T> key) {
        final Object data = getData(key.name());
        return data != null ? key.cast(data) : null;
    }
}

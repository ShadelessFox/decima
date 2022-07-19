package com.shade.decima.model.app;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

import javax.swing.*;
import java.util.Objects;

public record DataKey<T>(@NotNull String name, @NotNull Class<T> type) {
    @NotNull
    public T cast(@Nullable Object value) {
        return type.cast(Objects.requireNonNull(value));
    }

    @NotNull
    public T get(@NotNull JComponent component) {
        return cast(component.getClientProperty(this));
    }
}

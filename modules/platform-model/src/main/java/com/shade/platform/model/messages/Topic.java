package com.shade.platform.model.messages;

import com.shade.util.NotNull;

public record Topic<T>(@NotNull String name, @NotNull Class<T> type) {
    @NotNull
    public static <T> Topic<T> create(@NotNull String name, @NotNull Class<T> type) {
        return new Topic<>(name, type);
    }
}

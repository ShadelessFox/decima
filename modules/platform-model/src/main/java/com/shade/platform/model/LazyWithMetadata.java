package com.shade.platform.model;

import com.shade.util.NotNull;

import java.util.function.Supplier;

public class LazyWithMetadata<T, M> extends Lazy<T> {
    private final M metadata;
    private final Class<? extends T> type;

    private LazyWithMetadata(@NotNull Supplier<T> supplier, @NotNull M metadata, @NotNull Class<? extends T> type) {
        super(supplier);
        this.metadata = metadata;
        this.type = type;
    }

    @NotNull
    public static <T, M> LazyWithMetadata<T, M> of(@NotNull Supplier<T> supplier, @NotNull M metadata, @NotNull Class<? extends T> type) {
        return new LazyWithMetadata<>(supplier, metadata, type);
    }

    @NotNull
    public M metadata() {
        return metadata;
    }

    @NotNull
    public Class<? extends T> type() {
        return type;
    }

    public boolean isLoaded() {
        return value != NO_INIT;
    }

    @Override
    public String toString() {
        return "Lazy[" + (value == NO_INIT ? "<not loaded>" : value) + ", metadata=" + metadata + ']';
    }
}

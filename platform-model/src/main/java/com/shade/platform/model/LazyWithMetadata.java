package com.shade.platform.model;

import com.shade.util.NotNull;

import java.util.function.Supplier;

public class LazyWithMetadata<T, M> extends Lazy<T> {
    private final M metadata;

    private LazyWithMetadata(@NotNull Supplier<T> supplier, @NotNull M metadata) {
        super(supplier);
        this.metadata = metadata;
    }

    @NotNull
    public static <T, M> LazyWithMetadata<T, M> of(@NotNull Supplier<T> supplier, @NotNull M metadata) {
        return new LazyWithMetadata<>(supplier, metadata);
    }

    @NotNull
    public M metadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "Lazy[" + (value == NO_INIT ? "<not loaded>" : value) + ", metadata=" + metadata + ']';
    }
}

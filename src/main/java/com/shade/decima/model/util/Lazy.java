package com.shade.decima.model.util;

import java.util.function.Supplier;

public class Lazy<T> implements Supplier<T> {
    private final Supplier<T> supplier;
    private boolean loaded;
    private T value;

    public Lazy(@NotNull Supplier<T> supplier) {
        this.supplier = supplier;
        this.loaded = false;
        this.value = null;
    }

    @NotNull
    public static <T> Lazy<T> of(@NotNull Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }

    @Override
    public T get() {
        if (!loaded) {
            value = supplier.get();
            loaded = true;
        }
        return value;
    }

    public <E extends Exception> void ifLoaded(@NotNull ThrowableConsumer<? super T, E> consumer) throws E {
        if (loaded) {
            consumer.accept(value);
        }
    }

    @Override
    public String toString() {
        return "Lazy{loaded=" + loaded + ", value=" + value + '}';
    }

    public interface ThrowableConsumer<T, E extends Exception> {
        void accept(@NotNull T t) throws E;
    }
}

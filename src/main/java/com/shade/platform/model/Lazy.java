package com.shade.platform.model;

import com.shade.util.NotNull;

import java.util.function.Supplier;

public class Lazy<T> implements Supplier<T> {
    protected static final Object NO_INIT = new Object();

    protected final Supplier<T> supplier;
    protected T value;

    @SuppressWarnings("unchecked")
    public Lazy(@NotNull Supplier<T> supplier) {
        this.supplier = supplier;
        this.value = (T) NO_INIT;
    }

    @NotNull
    public static <T> Lazy<T> of(@NotNull Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }

    @Override
    public T get() {
        if (value == NO_INIT) {
            synchronized (this) {
                if (value == NO_INIT) {
                    value = supplier.get();
                }
            }
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    public void clear() {
        synchronized (this) {
            value = (T) NO_INIT;
        }
    }

    @SuppressWarnings("unchecked")
    public <E extends Throwable> void clear(@NotNull ThrowableConsumer<? super T, E> consumer) throws E {
        if (value != NO_INIT) {
            synchronized (this) {
                if (value != NO_INIT) {
                    consumer.accept(value);
                    value = (T) NO_INIT;
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Lazy[" + (value == NO_INIT ? "<not loaded>" : value) + ']';
    }

    public interface ThrowableConsumer<T, E extends Throwable> {
        void accept(@NotNull T t) throws E;
    }
}

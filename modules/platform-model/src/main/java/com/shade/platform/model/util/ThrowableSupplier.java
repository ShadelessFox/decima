package com.shade.platform.model.util;

@FunctionalInterface
public interface ThrowableSupplier<T, X extends Throwable> {
    T get() throws X;
}

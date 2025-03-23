package com.shade.util;

@FunctionalInterface
public interface ThrowableFunction<T, R, E extends Exception> {
    R apply(T t) throws E;
}

package com.shade.util.hash.spi;

import com.shade.util.NotNull;

import java.util.ServiceLoader;

@Deprecated
public sealed interface Hasher {
    @NotNull
    static Iterable<Hasher> availableHashers() {
        return ServiceLoader.load(Hasher.class);
    }

    @NotNull
    String name();

    non-sealed interface ToInt extends Hasher {
        int calculate(@NotNull byte[] data);
    }

    non-sealed interface ToLong extends Hasher {
        long calculate(@NotNull byte[] data);
    }
}
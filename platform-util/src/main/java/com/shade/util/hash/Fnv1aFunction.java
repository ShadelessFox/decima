package com.shade.util.hash;

import com.shade.util.NotNull;

import java.util.Objects;

final class Fnv1aFunction extends HashFunction {
    static final HashFunction FNV1A = new Fnv1aFunction();

    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    @NotNull
    @Override
    public HashCode hash(@NotNull byte[] input, int off, int len) {
        Objects.checkFromIndexSize(off, len, input.length);
        long hash = FNV_OFFSET_BASIS;
        for (int i = off; i < len; i++) {
            hash ^= input[i];
            hash = hash * FNV_PRIME;
        }
        return HashCode.fromLong(hash);
    }
}

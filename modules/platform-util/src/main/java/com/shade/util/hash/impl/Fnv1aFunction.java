package com.shade.util.hash.impl;

import com.shade.util.NotNull;
import com.shade.util.hash.HashCode;
import com.shade.util.hash.HashFunction;

import java.util.Objects;

public class Fnv1aFunction implements HashFunction {
    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    @Override
    public int bits() {
        return 64;
    }

    @NotNull
    @Override
    public HashCode hashBytes(@NotNull byte[] input, int off, int len) {
        Objects.checkFromIndexSize(off, len, input.length);
        long hash = FNV_OFFSET_BASIS;
        for (int i = off; i < len; i++) {
            hash ^= input[i];
            hash = hash * FNV_PRIME;
        }
        return HashCode.fromLong(hash);
    }
}

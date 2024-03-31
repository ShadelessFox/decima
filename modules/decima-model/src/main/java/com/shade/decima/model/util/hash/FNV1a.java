package com.shade.decima.model.util.hash;

import com.shade.decima.model.util.hash.spi.Hasher;
import com.shade.util.NotNull;

/**
 * Implementation of the FNV-1a hash algorithm.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Fowler%E2%80%93Noll%E2%80%93Vo_hash_function">Fowler–Noll–Vo hash function</a>
 */
public class FNV1a {
    public static class Provider implements Hasher.ToLong {
        @Override
        public @NotNull String name() {
            return "FNV-1a";
        }

        @Override
        public long calculate(@NotNull byte[] data) {
            return FNV1a.calculate(data);
        }
    }

    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    private FNV1a() {
        // prevents instantiation
    }

    public static long calculate(@NotNull byte[] data) {
        long hash = FNV_OFFSET_BASIS;
        for (byte b : data) {
            hash ^= b;
            hash = hash * FNV_PRIME;
        }
        return hash;
    }
}

package com.shade.util.hash;

import com.shade.util.NotNull;
import com.shade.util.hash.impl.Crc32Function;
import com.shade.util.hash.impl.Fnv1aFunction;
import com.shade.util.hash.impl.Murmur3Function;

public final class Hashing {
    // NOTE: Should be moved somewhere else. This is a general purpose class.
    private static final HashFunction decimaMurmur3 = murmur3(42);
    private static final HashFunction decimaCrc32 = crc32(0, 0x82F63B78);

    private static final Fnv1aFunction fnv1a = new Fnv1aFunction();

    private Hashing() {
    }

    @NotNull
    public static HashFunction murmur3(int seed) {
        return new Murmur3Function(seed);
    }

    @NotNull
    public static HashFunction crc32(int initial, int polynomial) {
        return new Crc32Function(initial, polynomial);
    }

    @NotNull
    public static HashFunction fnv1a() {
        return fnv1a;
    }

    @NotNull
    public static HashFunction decimaMurmur3() {
        return decimaMurmur3;
    }

    @NotNull
    public static HashFunction decimaCrc32() {
        return decimaCrc32;
    }
}

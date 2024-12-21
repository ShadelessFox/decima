package com.shade.util.hash.impl;

import com.shade.util.NotNull;
import com.shade.util.hash.HashCode;
import com.shade.util.hash.HashFunction;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public class Murmur3Function implements HashFunction {
    private final int seed;

    public Murmur3Function(int seed) {
        this.seed = seed;
    }

    @NotNull
    @Override
    public HashCode hashBytes(@NotNull byte[] input, int off, int len) {
        return HashCode.fromBytes(mmh3(input, off, len, seed));
    }

    @Override
    public int bits() {
        return 128;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Murmur3Function that)) return false;
        return seed == that.seed;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(seed);
    }

    @Override
    public String toString() {
        return "Murmur3Function{seed=" + seed + '}';
    }

    @NotNull
    private static byte[] mmh3(byte[] data, int off, int len, long seed) {
        var src = ByteBuffer.wrap(data, off, len).order(ByteOrder.LITTLE_ENDIAN);
        var dst = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);

        var h1 = seed;
        var h2 = seed;

        while (src.remaining() >= 16) {
            h1 = (Long.rotateLeft(h1 ^ mixK1(src.getLong()), 27) + h2) * 5 + 0x52dce729;
            h2 = (Long.rotateLeft(h2 ^ mixK2(src.getLong()), 31) + h1) * 5 + 0x38495ab5;
        }

        if (src.hasRemaining()) {
            dst.put(src);
            h1 ^= mixK1(dst.getLong(0));
            h2 ^= mixK2(dst.getLong(8));
        }

        h1 ^= len;
        h2 ^= len;

        h1 += h2;
        h2 += h1;

        h1 = fmix64(h1);
        h2 = fmix64(h2);

        h1 += h2;
        h2 += h1;

        return dst.putLong(0, h1).putLong(8, h2).array();
    }

    private static long mixK1(long k1) {
        return Long.rotateLeft(k1 * 0x87c37b91114253d5L, 31) * 0x4cf5ad432745937fL;
    }

    private static long mixK2(long k2) {
        return Long.rotateLeft(k2 * 0x4cf5ad432745937fL, 33) * 0x87c37b91114253d5L;
    }

    private static long fmix64(long hash) {
        hash ^= (hash >>> 33);
        hash *= 0xff51afd7ed558ccdL;
        hash ^= (hash >>> 33);
        hash *= 0xc4ceb9fe1a85ec53L;
        hash ^= (hash >>> 33);
        return hash;
    }
}

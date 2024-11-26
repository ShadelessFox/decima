package com.shade.util.hash.impl;

import com.shade.util.NotNull;
import com.shade.util.hash.HashCode;
import com.shade.util.hash.HashFunction;

import java.util.Objects;

public final class Crc32Function implements HashFunction {
    private final int[] lookup;
    private final int init;
    private final int poly;

    public Crc32Function(int init, int poly) {
        this.lookup = generateLookupTable(poly);
        this.init = init;
        this.poly = poly;
    }

    @Override
    public int bits() {
        return 32;
    }

    @NotNull
    @Override
    public HashCode hashBytes(@NotNull byte[] input, int off, int len) {
        Objects.checkFromIndexSize(off, len, input.length);
        int crc = init;
        for (int i = 0; i < len; i++) {
            byte b = input[i + off];
            crc = lookup[(crc ^ b) & 0xff] ^ (crc >>> 8);
        }
        return HashCode.fromInt(crc & ~0x80000000);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Crc32Function that)) return false;
        return init == that.init && poly == that.poly;
    }

    @Override
    public int hashCode() {
        return Objects.hash(init, poly);
    }

    @Override
    public String toString() {
        return "Crc32Function{init=" + init + ", poly=" + poly + '}';
    }

    @NotNull
    private static int[] generateLookupTable(int polynomial) {
        int[] lookup = new int[256];
        for (int i = 0; i < lookup.length; i++) {
            int r = i;
            for (int j = 0; j < 8; j++) {
                r = (r >>> 1) ^ ((r & 1) != 0 ? polynomial : 0);
            }
            lookup[i] = r;
        }
        return lookup;
    }
}

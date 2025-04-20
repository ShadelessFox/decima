package com.shade.util.hash;

import com.shade.util.NotNull;

import java.util.Objects;

final class Crc32Function extends HashFunction {
    static final HashFunction CRC32C = new Crc32Function(0, 0x82F63B78);

    private final int[] lookup;
    private final int init;

    Crc32Function(int init, int poly) {
        this.lookup = generateLookupTable(poly);
        this.init = init;
    }

    @NotNull
    @Override
    public HashCode hash(@NotNull byte[] input, int off, int len) {
        Objects.checkFromIndexSize(off, len, input.length);
        int crc = init;
        for (int i = 0; i < len; i++) {
            byte b = input[i + off];
            crc = lookup[(crc ^ b) & 0xff] ^ (crc >>> 8);
        }
        return HashCode.fromInt(crc & ~0x80000000);
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

package com.shade.decima.model.util.hash;

import com.shade.util.NotNull;

/**
 * A re-implementation of {@link java.util.zip.CRC32C} but with {@code 0} as a default seed.
 */
public class CRC32C {
    private static final int[] LOOKUP = new int[256];

    static {
        for (int i = 0; i < LOOKUP.length; i++) {
            int r = i;

            for (int j = 0; j < 8; j++) {
                r = (r >>> 1) ^ ((r & 1) != 0 ? 0x82F63B78 : 0);
            }

            LOOKUP[i] = r;
        }
    }

    private CRC32C() {
        // prevents instantiation
    }

    public static int calculate(@NotNull byte[] data) {
        int crc = 0;
        for (byte b : data) {
            crc = LOOKUP[(crc ^ b) & 0xff] ^ (crc >>> 8);
        }
        return crc & ~0x80000000;
    }
}

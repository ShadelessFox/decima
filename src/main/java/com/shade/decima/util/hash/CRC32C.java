package com.shade.decima.util.hash;

import com.shade.decima.util.NotNull;

import java.util.zip.Checksum;

/**
 * A re-implementation of {@link java.util.zip.CRC32C} but with {@code 0} as a default seed.
 */
public class CRC32C implements Checksum {
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

    private int crc = 0;

    public static long calculate(@NotNull byte[] data) {
        final CRC32C crc = new CRC32C();
        crc.update(data);
        return crc.getValue();
    }

    @Override
    public void update(int b) {
        crc = LOOKUP[(crc ^ b) & 0xff] ^ (crc >>> 8);
    }

    @Override
    public void update(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new ArrayIndexOutOfBoundsException();
        }
        for (int i = off; i < off + len; i++) {
            update(b[i]);
        }
    }

    @Override
    public long getValue() {
        return crc & ~0x80000000;
    }

    @Override
    public void reset() {
        crc = 0;
    }
}

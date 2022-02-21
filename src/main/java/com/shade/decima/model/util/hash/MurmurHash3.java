package com.shade.decima.model.util.hash;

import com.shade.decima.model.util.IOUtils;
import com.shade.decima.model.util.NotNull;

/**
 * MurmurHash3 was written by Austin Appleby, and is placed in the public domain.
 * The author hereby disclaims copyright to this source code.
 * <p>
 * See <a href=https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp>https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp</a>
 */
public class MurmurHash3 {
    // Constants for 128-bit MurmurHash3 variant
    private static final long C1 = 0x87c37b91114253d5L;
    private static final long C2 = 0x4cf5ad432745937fL;
    private static final int R1 = 31;
    private static final int R2 = 27;
    private static final int R3 = 33;
    private static final int M = 5;
    private static final int N1 = 0x52dce729;
    private static final int N2 = 0x38495ab5;

    @NotNull
    public static long[] mmh3(@NotNull byte[] data) {
        return mmh3(data, 0, data.length);
    }

    @NotNull
    public static long[] mmh3(@NotNull byte[] data, int offset, int length) {
        return mmh3(data, offset, length, 0x2A /* A seed used by Decima */);
    }

    @NotNull
    public static long[] mmh3(@NotNull byte[] data, int offset, int length, long seed) {
        long h1 = seed;
        long h2 = seed;
        final int blocks = length >> 4;

        for (int i = 0; i < blocks; i++) {
            final int index = offset + (i << 4);
            long k1 = IOUtils.toLong(data, index);
            long k2 = IOUtils.toLong(data, index + 8);

            // mix functions for k1
            k1 *= C1;
            k1 = Long.rotateLeft(k1, R1);
            k1 *= C2;
            h1 ^= k1;
            h1 = Long.rotateLeft(h1, R2);
            h1 += h2;
            h1 = h1 * M + N1;

            // mix functions for k2
            k2 *= C2;
            k2 = Long.rotateLeft(k2, R3);
            k2 *= C1;
            h2 ^= k2;
            h2 = Long.rotateLeft(h2, R1);
            h2 += h1;
            h2 = h2 * M + N2;
        }

        // tail
        long k1 = 0;
        long k2 = 0;
        final int index = offset + (blocks << 4);
        switch (offset + length - index) {
            case 15:
                k2 ^= ((long) data[index + 14] & 0xff) << 48;
                /* fall-through */
            case 14:
                k2 ^= ((long) data[index + 13] & 0xff) << 40;
                /* fall-through */
            case 13:
                k2 ^= ((long) data[index + 12] & 0xff) << 32;
                /* fall-through */
            case 12:
                k2 ^= ((long) data[index + 11] & 0xff) << 24;
                /* fall-through */
            case 11:
                k2 ^= ((long) data[index + 10] & 0xff) << 16;
                /* fall-through */
            case 10:
                k2 ^= ((long) data[index + 9] & 0xff) << 8;
                /* fall-through */
            case 9:
                k2 ^= data[index + 8] & 0xff;
                k2 *= C2;
                k2 = Long.rotateLeft(k2, R3);
                k2 *= C1;
                h2 ^= k2;
                /* fall-through */
            case 8:
                k1 ^= ((long) data[index + 7] & 0xff) << 56;
                /* fall-through */
            case 7:
                k1 ^= ((long) data[index + 6] & 0xff) << 48;
                /* fall-through */
            case 6:
                k1 ^= ((long) data[index + 5] & 0xff) << 40;
                /* fall-through */
            case 5:
                k1 ^= ((long) data[index + 4] & 0xff) << 32;
                /* fall-through */
            case 4:
                k1 ^= ((long) data[index + 3] & 0xff) << 24;
                /* fall-through */
            case 3:
                k1 ^= ((long) data[index + 2] & 0xff) << 16;
                /* fall-through */
            case 2:
                k1 ^= ((long) data[index + 1] & 0xff) << 8;
                /* fall-through */
            case 1:
                k1 ^= data[index] & 0xff;
                k1 *= C1;
                k1 = Long.rotateLeft(k1, R1);
                k1 *= C2;
                h1 ^= k1;
                break;
        }

        // finalization
        h1 ^= length;
        h2 ^= length;

        h1 += h2;
        h2 += h1;

        h1 = fmix64(h1);
        h2 = fmix64(h2);

        h1 += h2;
        h2 += h1;

        return new long[]{h1, h2};
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

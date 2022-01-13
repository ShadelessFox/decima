package com.shade.decima.util;

import com.shade.decima.util.hash.CRC32C;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;

public final class IOUtils {
    private IOUtils() {
    }

    @NotNull
    public static ByteBuffer readExact(@NotNull ReadableByteChannel channel, int capacity) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN);
        channel.read(buffer);
        return buffer.position(0);
    }

    @NotNull
    public static byte[] getBytesExact(@NotNull ByteBuffer buffer, int size, int checksum) {
        final byte[] data = getBytesExact(buffer, size);
        if (checksum != CRC32C.calculate(data)) {
            throw new IllegalArgumentException("Data is corrupted (mismatched checksum)");
        }
        return data;
    }

    @NotNull
    public static byte[] getBytesExact(@NotNull ByteBuffer buffer, int size) {
        final byte[] bytes = new byte[size];
        buffer.get(bytes);
        return bytes;
    }

    @NotNull
    public static byte[] toByteArray(@NotNull int[] src) {
        final byte[] dst = new byte[src.length * 4];
        for (int i = 0; i < src.length; i++) {
            dst[i * 4] = (byte) (src[i] & 0xff);
            dst[i * 4 + 1] = (byte) (src[i] >> 8 & 0xff);
            dst[i * 4 + 2] = (byte) (src[i] >> 16 & 0xff);
            dst[i * 4 + 3] = (byte) (src[i] >> 24 & 0xff);
        }
        return dst;
    }

    @NotNull
    public static byte[] toByteArray(@NotNull long[] src) {
        final byte[] dst = new byte[src.length * 8];
        for (int i = 0; i < src.length; i++) {
            dst[i * 8] = (byte) (src[i] & 0xff);
            dst[i * 8 + 1] = (byte) (src[i] >>> 8 & 0xff);
            dst[i * 8 + 2] = (byte) (src[i] >>> 16 & 0xff);
            dst[i * 8 + 3] = (byte) (src[i] >>> 24 & 0xff);
            dst[i * 8 + 4] = (byte) (src[i] >>> 32 & 0xff);
            dst[i * 8 + 5] = (byte) (src[i] >>> 40 & 0xff);
            dst[i * 8 + 6] = (byte) (src[i] >>> 48 & 0xff);
            dst[i * 8 + 7] = (byte) (src[i] >>> 56 & 0xff);
        }
        return dst;
    }

    public static long toLong(@NotNull byte[] src, int index) {
        return
            (long) (src[index] & 0xff) |
                (long) (src[index + 1] & 0xff) << 8 |
                (long) (src[index + 2] & 0xff) << 16 |
                (long) (src[index + 3] & 0xff) << 24 |
                (long) (src[index + 4] & 0xff) << 32 |
                (long) (src[index + 5] & 0xff) << 40 |
                (long) (src[index + 6] & 0xff) << 48 |
                (long) (src[index + 7] & 0xff) << 56;
    }
}

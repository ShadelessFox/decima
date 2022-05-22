package com.shade.decima.model.util;

import com.shade.decima.model.util.hash.CRC32C;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

public final class IOUtils {
    private static final String[] UNIT_NAMES = {"bytes", "KiB", "MiB", "GiB", "TiB", "PiB"};

    private IOUtils() {
    }

    @NotNull
    public static Reader newCompressedReader(@NotNull Path path) throws IOException {
        final Path gzip = Path.of(path + ".gz");

        if (Files.exists(gzip)) {
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(gzip.toFile())), StandardCharsets.UTF_8));
        } else {
            return Files.newBufferedReader(path);
        }
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
    public static String getString(@NotNull ByteBuffer buffer, int length) {
        return new String(getBytesExact(buffer, length), StandardCharsets.UTF_8);
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

    public static void put(@NotNull byte[] dst, int index, int value) {
        dst[index] = (byte) (value & 0xff);
        dst[index + 1] = (byte) (value >> 8 & 0xff);
        dst[index + 2] = (byte) (value >> 16 & 0xff);
        dst[index + 3] = (byte) (value >> 24 & 0xff);
    }

    public static void put(@NotNull byte[] dst, int index, long value) {
        dst[index] = (byte) (value & 0xff);
        dst[index + 1] = (byte) (value >>> 8 & 0xff);
        dst[index + 2] = (byte) (value >>> 16 & 0xff);
        dst[index + 3] = (byte) (value >>> 24 & 0xff);
        dst[index + 4] = (byte) (value >>> 32 & 0xff);
        dst[index + 5] = (byte) (value >>> 40 & 0xff);
        dst[index + 6] = (byte) (value >>> 48 & 0xff);
        dst[index + 7] = (byte) (value >>> 56 & 0xff);
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

    @NotNull
    public static String formatSize(long size) {
        for (int i = 0; ; i++) {
            final double result = (double) size / (1024 << (10 * i++));

            if (result < 1024 || i == UNIT_NAMES.length) {
                return String.format("%.2f %s", result, UNIT_NAMES[i]);
            }
        }
    }
}

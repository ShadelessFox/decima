package com.shade.platform.model.util;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public final class IOUtils {
    private static final NumberFormat UNIT_FORMAT = new DecimalFormat("#.## ");
    private static final String[] UNIT_NAMES = {"B", "kB", "mB", "gB", "tB", "pB", "eB"};
    private static final byte[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private IOUtils() {
        // prevents instantiation
    }

    @NotNull
    public static <T> T getNotNull(@NotNull Preferences preferences, @NotNull String key, @NotNull Function<String, ? extends T> mapper) {
        return mapper.apply(Objects.requireNonNull(preferences.get(key, null)));
    }

    @NotNull
    public static String getNotNull(@NotNull Preferences preferences, @NotNull String key) {
        return getNotNull(preferences, key, Function.identity());
    }

    @NotNull
    public static Preferences[] children(@NotNull Preferences node) {
        return Stream.of(unchecked(node::childrenNames))
            .map(node::node)
            .toArray(Preferences[]::new);
    }

    @Nullable
    public static <T> T getNullable(@NotNull Preferences preferences, @NotNull String key, @NotNull Function<String, ? extends T> mapper) {
        final String value = preferences.get(key, null);
        if (value != null) {
            return mapper.apply(value);
        } else {
            return null;
        }
    }

    @Nullable
    public static String getNullable(@NotNull Preferences preferences, @NotNull String key) {
        return getNullable(preferences, key, Function.identity());
    }

    @NotNull
    public static String getFilename(@NotNull String path) {
        final int index = path.lastIndexOf('/');

        if (index < 0) {
            return path;
        } else {
            return path.substring(index + 1);
        }
    }

    @NotNull
    public static String getBasename(@NotNull String path) {
        final int index = path.indexOf('.');

        if (index < 0) {
            return path;
        } else {
            return path.substring(0, index);
        }
    }

    @NotNull
    public static String getExtension(@NotNull String filename) {
        return getExtension(filename, true);
    }

    @NotNull
    public static String getFullExtension(@NotNull String filename) {
        return getExtension(filename, false);
    }

    @NotNull
    private static String getExtension(@NotNull String filename, boolean lastPartOnly) {
        final int index = lastPartOnly ? filename.lastIndexOf('.') : filename.indexOf('.');

        if (index > 0 && index < filename.length() - 1) {
            return filename.substring(index + 1);
        } else {
            return "";
        }
    }

    @NotNull
    public static BufferedReader newCompressedReader(@NotNull Path path) throws IOException {
        if (isCompressed(path)) {
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(path.toFile())), StandardCharsets.UTF_8));
        } else {
            return Files.newBufferedReader(path);
        }
    }

    public static boolean isCompressed(@NotNull Path path) {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            final ByteBuffer buffer = IOUtils.readExact(channel, 2);
            final int magic = buffer.getShort() & 0xffff;
            return magic == GZIPInputStream.GZIP_MAGIC;
        } catch (IOException ignored) {
            return false;
        }
    }

    @NotNull
    public static ByteBuffer readExact(@NotNull ReadableByteChannel channel, int capacity) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN);
        channel.read(buffer);
        return buffer.position(0);
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

    public static float getHalfFloat(@NotNull ByteBuffer buffer) {
        return halfToFloat(buffer.getShort() & 0xffff);
    }

    public static float getHalfFloat(@NotNull ByteBuffer buffer, int index) {
        return halfToFloat(buffer.getShort(index) & 0xffff);
    }

    public static void putHalfFloat(@NotNull ByteBuffer buffer, float value) {
        buffer.putShort((short) floatToHalf(value));
    }

    @NotNull
    public static BigInteger getUInt128(@NotNull ByteBuffer buffer) {
        final byte[] data = new byte[16];
        buffer.slice().order(ByteOrder.BIG_ENDIAN).get(data);
        buffer.position(buffer.position() + 16);
        return new BigInteger(1, data);
    }

    public static void putUInt128(@NotNull ByteBuffer buffer, @NotNull BigInteger value) {
        final byte[] data = value.toByteArray();
        if (data.length > 16) {
            throw new IllegalArgumentException("The number is too big: " + value);
        }
        buffer.slice().order(ByteOrder.BIG_ENDIAN).put(data);
        buffer.position(buffer.position() + 16);
    }

    @NotNull
    public static <T> T[] getObjects(@NotNull ByteBuffer buffer, int count, @NotNull IntFunction<T[]> generator, @NotNull Function<ByteBuffer, T> reader) {
        final T[] output = generator.apply(count);
        for (int i = 0; i < output.length; i++) {
            output[i] = reader.apply(buffer);
        }
        return output;
    }

    @NotNull
    public static byte[] toByteArray(@NotNull int... src) {
        final byte[] dst = new byte[src.length * 4];
        for (int i = 0; i < src.length; i++) {
            dst[i * 4] = (byte) (src[i] & 0xff);
            dst[i * 4 + 1] = (byte) (src[i] >> 8 & 0xff);
            dst[i * 4 + 2] = (byte) (src[i] >> 16 & 0xff);
            dst[i * 4 + 3] = (byte) (src[i] >> 24 & 0xff);
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

    public static int alignUp(int value, int to) {
        return (value + to - 1) / to * to;
    }

    public static int wrapAround(int index, int max) {
        return (index % max + max) % max;
    }

    public static <T> int indexOf(@NotNull T[] array, @NotNull T value) {
        for (int i = 0; i < array.length; i++) {
            if (value.equals(array[i])) {
                return i;
            }
        }

        return -1;
    }

    public static int indexOf(@NotNull int[] array, int value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                return i;
            }
        }

        return -1;
    }

    public static int indexOf(@NotNull short[] array, short value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                return i;
            }
        }

        return -1;
    }

    public static short signExtend(int value, int bits) {
        if ((value & (1 << bits - 1)) > 0) {
            return (short) ((value | -1 << bits - 1) & 0xffff);
        } else {
            return (short) (value & 0xffff);
        }
    }

    @NotNull
    public static String formatSize(long size) {
        double result = size;
        int unit = 0;

        while (result >= 1024 && unit < UNIT_NAMES.length - 1) {
            result /= 1024;
            unit += 1;
        }

        return UNIT_FORMAT.format(result) + UNIT_NAMES[unit];
    }

    public static void exec(@NotNull Object... args) throws IOException, InterruptedException {
        final List<String> command = Arrays.stream(args)
            .map(Object::toString)
            .toList();

        final Process process = new ProcessBuilder()
            .command(command)
            .start();

        final int rc = process.waitFor();

        if (rc != 0) {
            final String stdout;
            final String stderr;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                stdout = reader.lines().collect(Collectors.joining("\n"));
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                stderr = reader.lines().collect(Collectors.joining("\n"));
            }

            final StringBuilder sb = new StringBuilder("Process finished with exit code ").append(rc);

            if (!stdout.isEmpty()) {
                sb.append("\n\nOutput:\n").append(stdout);
            }

            if (!stderr.isEmpty()) {
                sb.append("\n\nError:\n").append(stderr);
            }

            throw new IOException(sb.toString());
        }
    }

    @NotNull
    public static <T> T last(@NotNull List<? extends T> list) {
        return list.get(list.size() - 1);
    }

    public static <T, E extends Throwable> T unchecked(@NotNull ThrowableSupplier<T, E> supplier) {
        try {
            return supplier.get();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // https://stackoverflow.com/a/6162687
    public static float halfToFloat(int value) {
        int mant = value & 0x03ff;
        int exp = value & 0x7c00;

        if (exp == 0x7c00) {
            exp = 0x3fc00;
        } else if (exp != 0) {
            exp += 0x1c000;
            if (mant == 0 && exp > 0x1c400) {
                return Float.intBitsToFloat((value & 0x8000) << 16 | exp << 13 | 0x3ff);
            }
        } else if (mant != 0) {
            exp = 0x1c400;
            do {
                mant <<= 1;
                exp -= 0x400;
            } while ((mant & 0x400) == 0);
            mant &= 0x3ff;
        }

        return Float.intBitsToFloat((value & 0x8000) << 16 | (exp | mant) << 13);
    }

    // https://stackoverflow.com/a/6162687
    public static int floatToHalf(float value) {
        int bits = Float.floatToIntBits(value);
        int sign = bits >>> 16 & 0x8000;
        int val = (bits & 0x7fffffff) + 0x1000;

        if (val >= 0x47800000) {
            if ((bits & 0x7fffffff) >= 0x47800000) {
                if (val < 0x7f800000) {
                    return sign | 0x7c00;
                } else {
                    return sign | 0x7c00 | (bits & 0x007fffff) >>> 13;
                }
            } else {
                return sign | 0x7bff;
            }
        }

        if (val >= 0x38800000) {
            return sign | val - 0x38000000 >>> 13;
        } else if (val < 0x33000000) {
            return sign;
        } else {
            val = (bits & 0x7fffffff) >>> 23;
            return sign | ((bits & 0x7fffff | 0x800000) + (0x800000 >>> val - 102) >>> 126 - val);
        }
    }

    @NotNull
    public static String toHexDigits(byte value) {
        final byte[] buf = new byte[2];
        toHexDigits(value, buf, 0);
        return new String(buf, StandardCharsets.ISO_8859_1);
    }

    @NotNull
    public static String toHexDigits(int value, @NotNull ByteOrder order) {
        final byte[] buf = new byte[8];
        toHexDigits(value, buf, 0, order);
        return new String(buf, StandardCharsets.ISO_8859_1);
    }

    @NotNull
    public static String toHexDigits(long value, @NotNull ByteOrder order) {
        final byte[] buf = new byte[16];
        toHexDigits(value, buf, 0, order);
        return new String(buf, StandardCharsets.ISO_8859_1);
    }

    public static void toHexDigits(byte value, @NotNull byte[] buf, int off) {
        Objects.checkFromIndexSize(off, 2, buf.length);

        buf[off] = toHighHexDigit(value);
        buf[off + 1] = toLowHexDigit(value);
    }

    public static void toHexDigits(int value, @NotNull byte[] buf, int off, @NotNull ByteOrder order) {
        Objects.checkFromIndexSize(off, 8, buf.length);

        if (order == ByteOrder.LITTLE_ENDIAN) {
            toHexDigits((byte) (value), buf, off);
            toHexDigits((byte) (value >>> 8), buf, off + 2);
            toHexDigits((byte) (value >>> 16), buf, off + 4);
            toHexDigits((byte) (value >>> 24), buf, off + 6);
        } else {
            toHexDigits((byte) (value >>> 24), buf, off);
            toHexDigits((byte) (value >>> 16), buf, off + 2);
            toHexDigits((byte) (value >>> 8), buf, off + 4);
            toHexDigits((byte) (value), buf, off + 6);
        }
    }

    public static void toHexDigits(long value, @NotNull byte[] buf, int off, @NotNull ByteOrder order) {
        Objects.checkFromIndexSize(off, 16, buf.length);

        if (order == ByteOrder.LITTLE_ENDIAN) {
            toHexDigits((int) (value), buf, off, order);
            toHexDigits((int) (value >>> 32), buf, off + 8, order);
        } else {
            toHexDigits((int) (value >>> 32), buf, off, order);
            toHexDigits((int) (value), buf, off + 8, order);
        }
    }

    private static byte toLowHexDigit(int i) {
        return DIGITS[i & 0xf];
    }

    private static byte toHighHexDigit(int i) {
        return DIGITS[i >>> 4 & 0xf];
    }

    public interface ThrowableSupplier<T, E extends Throwable> {
        T get() throws E;
    }
}

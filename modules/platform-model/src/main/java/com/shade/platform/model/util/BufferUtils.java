package com.shade.platform.model.util;

import com.shade.util.NotNull;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

public class BufferUtils {
    private BufferUtils() {
        // prevents instantiation
    }

    @NotNull
    public static ByteBuffer readFromChannel(@NotNull ByteChannel channel, int count) throws IOException {
        final ByteBuffer buffer = ByteBuffer
            .allocate(count)
            .order(ByteOrder.LITTLE_ENDIAN);
        if (channel.read(buffer) != count) {
            throw new IOException("Unexpected end of stream, expected " + count + " bytes but got " + buffer.position());
        }
        return buffer.flip();
    }

    @NotNull
    public static byte[] getBytes(@NotNull ByteBuffer buffer, int size) {
        final byte[] bytes = new byte[size];
        buffer.get(bytes);
        return bytes;
    }

    @NotNull
    public static String getString(@NotNull ByteBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            if (b == 0) {
                return sb.toString();
            }
            sb.append((char) b);
        }
        throw new IllegalArgumentException("Buffer has no remaining bytes");
    }

    @NotNull
    public static String getString(@NotNull ByteBuffer buffer, int length) {
        return new String(getBytes(buffer, length), StandardCharsets.UTF_8);
    }

    @NotNull
    public static String getPString(@NotNull ByteBuffer buffer) {
        return getString(buffer, buffer.getInt());
    }

    public static float getHalfFloat(@NotNull ByteBuffer buffer) {
        return MathUtils.halfToFloat(buffer.getShort());
    }

    public static float getHalfFloat(@NotNull ByteBuffer buffer, int index) {
        return MathUtils.halfToFloat(buffer.getShort(index));
    }

    public static void putHalfFloat(@NotNull ByteBuffer buffer, float value) {
        buffer.putShort((short) MathUtils.floatToHalf(value));
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
    public static <T> List<T> getStructs(@NotNull ByteBuffer buffer, int count, @NotNull Function<ByteBuffer, T> reader) {
        return IntStream.range(0, count)
            .mapToObj(i -> reader.apply(buffer))
            .toList();
    }

    @NotNull
    public static short[] getShorts(@NotNull ByteBuffer buffer, int count) {
        final short[] output = new short[count];
        buffer.asShortBuffer().get(output, 0, output.length);
        buffer.position(buffer.position() + Short.BYTES * count);
        return output;
    }

    @NotNull
    public static int[] getInts(@NotNull ByteBuffer buffer, int count) {
        final int[] output = new int[count];
        buffer.asIntBuffer().get(output, 0, output.length);
        buffer.position(buffer.position() + Integer.BYTES * count);
        return output;
    }

    @NotNull
    public static long[] getLongs(@NotNull ByteBuffer buffer, int count) {
        final long[] output = new long[count];
        buffer.asLongBuffer().get(output, 0, output.length);
        buffer.position(buffer.position() + Long.BYTES * count);
        return output;
    }

    @NotNull
    public static UUID getUUID(@NotNull ByteBuffer buffer) {
        long hi = buffer.getLong();
        long lo = buffer.getLong();
        return new UUID(hi, lo);
    }

    public static boolean getByteBoolean(@NotNull ByteBuffer buffer) {
        byte value = buffer.get();
        return switch (value) {
            case 0 -> false;
            case 1 -> true;
            default -> throw new IllegalArgumentException("Invalid boolean value: " + value);
        };
    }
}

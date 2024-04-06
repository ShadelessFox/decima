package com.shade.platform.model.util;

import com.shade.util.NotNull;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.function.IntFunction;

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

    public static int expectInt(@NotNull ByteBuffer buffer, int expected, @NotNull String message) {
        final int value = buffer.getInt();
        if (value != expected) {
            throw new IllegalStateException(message + ": expected " + expected + " but got " + value);
        }
        return value;
    }

    @NotNull
    public static byte[] getBytes(@NotNull ByteBuffer buffer, int size) {
        final byte[] bytes = new byte[size];
        buffer.get(bytes);
        return bytes;
    }

    @NotNull
    public static String getString(@NotNull ByteBuffer buffer, int length) {
        return new String(getBytes(buffer, length), StandardCharsets.UTF_8);
    }

    public static float getHalfFloat(@NotNull ByteBuffer buffer) {
        return MathUtils.halfToFloat(buffer.getShort() & 0xffff);
    }

    public static float getHalfFloat(@NotNull ByteBuffer buffer, int index) {
        return MathUtils.halfToFloat(buffer.getShort(index) & 0xffff);
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
    public static float[] getFloats(@NotNull ByteBuffer buffer, int count) {
        final float[] output = new float[count];
        buffer.asFloatBuffer().get(output, 0, output.length);
        buffer.position(buffer.position() + Float.BYTES * count);
        return output;
    }
}

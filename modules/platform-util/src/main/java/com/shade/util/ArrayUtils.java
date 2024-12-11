package com.shade.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

public final class ArrayUtils {
    private static final VarHandle asShortLittleEndian = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle asIntLittleEndian = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle asLongLittleEndian = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle asFloatLittleEndian = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle asDoubleLittleEndian = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.LITTLE_ENDIAN);

    private ArrayUtils() {
    }

    public static short getShort(@NotNull byte[] array, int index) {
        return (short) asShortLittleEndian.get(array, index);
    }

    public static int getInt(@NotNull byte[] array, int index) {
        return (int) asIntLittleEndian.get(array, index);
    }

    public static long getLong(@NotNull byte[] array, int index) {
        return (long) asLongLittleEndian.get(array, index);
    }

    public static float getFloat(@NotNull byte[] array, int index) {
        return (float) asFloatLittleEndian.get(array, index);
    }

    public static double getDouble(@NotNull byte[] array, int index) {
        return (double) asDoubleLittleEndian.get(array, index);
    }
}

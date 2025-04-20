package com.shade.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

public final class ArrayUtils {
    private static final VarHandle asShortLE = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle asShortBE = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle asIntLE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle asIntBE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle asLongLE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle asLongBE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle asFloatLE = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle asFloatBE = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle asDoubleLE = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle asDoubleBE = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.BIG_ENDIAN);

    private ArrayUtils() {
    }

    public static short getShort(@NotNull byte[] array, int index, @NotNull ByteOrder order) {
        var handle = order == ByteOrder.LITTLE_ENDIAN ? asShortLE : asShortBE;
        return (short) handle.get(array, index);
    }

    public static int getInt(@NotNull byte[] array, int index, @NotNull ByteOrder order) {
        var handle = order == ByteOrder.LITTLE_ENDIAN ? asIntLE : asIntBE;
        return (int) handle.get(array, index);
    }

    public static long getLong(@NotNull byte[] array, int index, @NotNull ByteOrder order) {
        var handle = order == ByteOrder.LITTLE_ENDIAN ? asLongLE : asLongBE;
        return (long) handle.get(array, index);
    }

    public static float getFloat(@NotNull byte[] array, int index, @NotNull ByteOrder order) {
        var handle = order == ByteOrder.LITTLE_ENDIAN ? asFloatLE : asFloatBE;
        return (float) handle.get(array, index);
    }

    public static double getDouble(@NotNull byte[] array, int index, @NotNull ByteOrder order) {
        var handle = order == ByteOrder.LITTLE_ENDIAN ? asDoubleLE : asDoubleBE;
        return (double) handle.get(array, index);
    }
}

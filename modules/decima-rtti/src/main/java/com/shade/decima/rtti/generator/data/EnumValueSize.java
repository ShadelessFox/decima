package com.shade.decima.rtti.generator.data;

import com.shade.util.NotNull;

public enum EnumValueSize {
    INT8(byte.class, Byte.BYTES),
    INT16(short.class, Short.BYTES),
    INT32(int.class, Integer.BYTES);

    private final Class<? extends Number> type;
    private final int bytes;

    EnumValueSize(@NotNull Class<? extends Number> type, int bytes) {
        this.type = type;
        this.bytes = bytes;
    }

    @NotNull
    public Class<? extends Number> type() {
        return type;
    }

    public int bytes() {
        return bytes;
    }
}

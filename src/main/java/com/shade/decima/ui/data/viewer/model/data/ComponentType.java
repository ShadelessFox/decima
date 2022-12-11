package com.shade.decima.ui.data.viewer.model.data;

import com.shade.util.NotNull;

public enum ComponentType {
    INT8(Byte.BYTES, byte.class),
    UINT8(Byte.BYTES, byte.class),
    INT16(Short.BYTES, short.class),
    UINT16(Short.BYTES, short.class),
    INT32(Integer.BYTES, int.class),
    UINT32(Integer.BYTES, int.class),
    FLOAT32(Float.BYTES, float.class),
    FLOAT64(Double.BYTES, double.class),
    X10Y10Z10W2Normalized(Integer.BYTES, int.class),
    X10Y10Z10W2UNorm(Integer.BYTES, int.class),
    ;

    private final int size;
    private final Class<?> type;

    ComponentType(int size, @NotNull Class<?> type) {
        this.size = size;
        this.type = type;
    }

    public int getSize() {
        return size;
    }

    @NotNull
    public Class<?> getType() {
        return type;
    }
}

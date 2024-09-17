package com.shade.decima.rtti.generator.data;

import com.shade.util.NotNull;

public enum EnumValueSize {
    INT8(byte.class),
    INT16(short.class),
    INT32(int.class);

    private final Class<? extends Number> type;

    EnumValueSize(@NotNull Class<? extends Number> type) {
        this.type = type;
    }

    @NotNull
    public Class<? extends Number> type() {
        return type;
    }
}

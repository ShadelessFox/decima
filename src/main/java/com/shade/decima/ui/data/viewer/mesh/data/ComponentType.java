package com.shade.decima.ui.data.viewer.mesh.data;

import com.shade.util.NotNull;

public enum ComponentType {
    BYTE(5120, Byte.BYTES, Byte.class),
    UNSIGNED_BYTE(5121, Byte.BYTES, Byte.class),
    SHORT(5122, Short.BYTES, Short.class),
    UNSIGNED_SHORT(5123, Short.BYTES, Short.class),
    INT(5124, Integer.BYTES, Integer.class),
    UNSIGNED_INT(5125, Integer.BYTES, Integer.class),
    FLOAT(5126, Float.BYTES, Float.class);

    private final int id;
    private final int size;
    private final Class<?> type;

    ComponentType(int id, int size, @NotNull Class<?> type) {
        this.id = id;
        this.size = size;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public int getSize() {
        return size;
    }

    public Class<?> getType() {
        return type;
    }
}

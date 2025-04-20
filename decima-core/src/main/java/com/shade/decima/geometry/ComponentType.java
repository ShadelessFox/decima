package com.shade.decima.geometry;

public enum ComponentType {
    BYTE(1), UNSIGNED_BYTE(1),
    SHORT(2), UNSIGNED_SHORT(2),
    INT(4), UNSIGNED_INT(4),
    HALF_FLOAT(2), FLOAT(4),
    INT_10_10_10_2(4), UNSIGNED_INT_10_10_10_2(4);

    private final int size;

    ComponentType(int size) {
        this.size = size;
    }

    public int size() {
        return size;
    }
}

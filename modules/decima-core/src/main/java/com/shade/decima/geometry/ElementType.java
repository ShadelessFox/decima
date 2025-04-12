package com.shade.decima.geometry;

public enum ElementType {
    SCALAR(1),
    VEC2(2),
    VEC3(3),
    VEC4(4),
    MAT2(4),
    MAT3(9),
    MAT4(16);

    private final int size;

    ElementType(int size) {
        this.size = size;
    }

    public int size() {
        return size;
    }
}

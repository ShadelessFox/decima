package com.shade.decima.ui.data.viewer.model.data;

import com.shade.util.NotNull;

public enum ElementType {
    SCALAR(1),
    VEC2(2),
    VEC3(3),
    VEC4(4),
    MAT4(16);

    private final int componentCount;

    ElementType(int componentCount) {
        this.componentCount = componentCount;
    }

    public int getComponentCount() {
        return componentCount;
    }

    public int getStride(@NotNull ComponentType componentType) {
        return componentCount * componentType.getSize();
    }
}
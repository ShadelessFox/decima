package com.shade.decima.geometry;

import java.nio.ByteBuffer;

public record Accessor(
    ByteBuffer buffer,
    ElementType elementType,
    ComponentType componentType,
    int offset,
    int count,
    int stride,
    boolean normalized
) {
    public Accessor {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be positive");
        }
        if (stride <= 0) {
            throw new IllegalArgumentException("stride must be positive");
        }
        if (normalized && (componentType == ComponentType.FLOAT || componentType == ComponentType.HALF_FLOAT)) {
            throw new IllegalArgumentException("normalized can only be used with integer component types");
        }
    }

    public Accessor(ByteBuffer buffer, ElementType elementType, ComponentType componentType, int offset, int count, int stride) {
        this(buffer, elementType, componentType, offset, count, stride, false);
    }

    public Accessor(ByteBuffer buffer, ElementType elementType, ComponentType componentType, int offset, int count) {
        this(buffer, elementType, componentType, offset, count, elementType.size() * componentType.size());
    }
}

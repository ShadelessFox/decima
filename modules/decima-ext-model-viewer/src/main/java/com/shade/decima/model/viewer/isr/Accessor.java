package com.shade.decima.model.viewer.isr;

import com.shade.gl.Attribute;
import com.shade.util.NotNull;

public record Accessor(
    @NotNull BufferView bufferView,
    @NotNull Attribute.ElementType elementType,
    @NotNull Attribute.ComponentType componentType,
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
        if (stride < 0) {
            throw new IllegalArgumentException("stride must be positive");
        }
    }
}

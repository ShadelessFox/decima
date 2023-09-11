package com.shade.decima.model.viewer.gl;

import com.shade.decima.model.viewer.isr.Accessor.ComponentType;
import com.shade.decima.model.viewer.isr.Primitive;
import com.shade.util.NotNull;

public record Attribute(
    @NotNull Primitive.Semantic semantic,
    @NotNull ComponentType type,
    int components,
    long offset,
    int stride,
    boolean normalized
) {
    public int glType() {
        return type.glType();
    }

    public int glSize() {
        if (type == ComponentType.INT_10_10_10_2 || type == ComponentType.UNSIGNED_INT_10_10_10_2) {
            return 4;
        } else {
            return components;
        }
    }
}

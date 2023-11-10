package com.shade.decima.model.viewer.isr;

import com.shade.gl.Attribute;
import com.shade.util.NotNull;

public record Accessor(
    @NotNull BufferView bufferView,
    @NotNull Attribute.ElementType elementType,
    @NotNull Attribute.ComponentType componentType,
    int offset,
    int count,
    boolean normalized
) {}

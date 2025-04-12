package com.shade.decima.geometry;

import com.shade.util.Nullable;

import java.util.List;

public record Mesh(@Nullable String name, List<Primitive> primitives) {
    public Mesh {
        primitives = List.copyOf(primitives);
    }
}

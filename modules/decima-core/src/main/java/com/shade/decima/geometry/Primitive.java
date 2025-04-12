package com.shade.decima.geometry;

import java.util.Map;

public record Primitive(Accessor indices, Map<Semantic, Accessor> vertices) {
    public Primitive {
        if (indices.componentType() != ComponentType.UNSIGNED_SHORT && indices.componentType() != ComponentType.UNSIGNED_INT) {
            throw new IllegalArgumentException("indices must be of type UNSIGNED_SHORT or UNSIGNED_INT");
        }
        if (indices.elementType() != ElementType.SCALAR) {
            throw new IllegalArgumentException("indices must be of element type SCALAR");
        }
        if (indices.normalized()) {
            throw new IllegalArgumentException("indices cannot be normalized");
        }
        vertices = Map.copyOf(vertices);
    }
}

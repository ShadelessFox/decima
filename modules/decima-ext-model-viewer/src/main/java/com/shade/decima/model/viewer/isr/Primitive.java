package com.shade.decima.model.viewer.isr;

import com.shade.util.NotNull;

import java.util.Map;

/**
 * Represents bare primitive that can be rendered.
 */
public record Primitive(@NotNull Map<Semantic, Accessor> attributes, @NotNull Accessor indices, int hash) {
    public enum Semantic {
        POSITION(Accessor.ElementType.VEC3),
        NORMAL(Accessor.ElementType.VEC3),
        TANGENT(Accessor.ElementType.VEC4),
        TEXTURE(Accessor.ElementType.VEC2),
        COLOR(Accessor.ElementType.VEC4),
        JOINTS(Accessor.ElementType.VEC4),
        WEIGHTS(Accessor.ElementType.VEC4);

        private final Accessor.ElementType elementType;

        Semantic(Accessor.ElementType elementType) {
            this.elementType = elementType;
        }

        @NotNull
        public Accessor.ElementType getElementType() {
            return elementType;
        }
    }
}

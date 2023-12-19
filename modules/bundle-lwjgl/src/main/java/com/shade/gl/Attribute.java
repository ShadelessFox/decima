package com.shade.gl;

import com.shade.util.NotNull;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_10_10_10_2;
import static org.lwjgl.opengl.GL30.GL_HALF_FLOAT;
import static org.lwjgl.opengl.GL33.GL_INT_2_10_10_10_REV;

public record Attribute(
    @NotNull Semantic semantic,
    @NotNull ComponentType type,
    int components,
    long offset,
    int stride,
    boolean normalized
) {
    public enum Semantic {
        POSITION(ElementType.VEC3),
        NORMAL(ElementType.VEC3),
        TANGENT(ElementType.VEC4),
        TEXTURE(ElementType.VEC2),
        COLOR(ElementType.VEC4),
        JOINTS(ElementType.VEC4),
        WEIGHTS(ElementType.VEC4);

        private final ElementType elementType;

        Semantic(@NotNull ElementType elementType) {
            this.elementType = elementType;
        }

        @NotNull
        public ElementType elementType() {
            return elementType;
        }
    }

    public enum ElementType {
        SCALAR(1),
        VEC2(2),
        VEC3(3),
        VEC4(4);

        private final int componentCount;

        ElementType(int componentCount) {
            this.componentCount = componentCount;
        }

        public int componentCount() {
            return componentCount;
        }
    }

    public enum ComponentType {
        BYTE(GL_BYTE, 1),
        UNSIGNED_BYTE(GL_UNSIGNED_BYTE, 1),
        SHORT(GL_SHORT, 2),
        UNSIGNED_SHORT(GL_UNSIGNED_SHORT, 2),
        INT(GL_INT, 4),
        UNSIGNED_INT(GL_UNSIGNED_INT, 4),
        HALF_FLOAT(GL_HALF_FLOAT, 2),
        FLOAT(GL_FLOAT, 4),
        INT_10_10_10_2(GL_INT_2_10_10_10_REV, 4),
        UNSIGNED_INT_10_10_10_2(GL_UNSIGNED_INT_10_10_10_2, 4);

        private final int glType;
        private final int glSize;

        ComponentType(int glType, int glSize) {
            this.glType = glType;
            this.glSize = glSize;
        }

        public int glType() {
            return glType;
        }

        public int glSize() {
            return glSize;
        }
    }

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

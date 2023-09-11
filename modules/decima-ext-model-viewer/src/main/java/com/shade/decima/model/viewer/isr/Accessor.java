package com.shade.decima.model.viewer.isr;

import com.shade.util.NotNull;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_10_10_10_2;
import static org.lwjgl.opengl.GL30.GL_HALF_FLOAT;
import static org.lwjgl.opengl.GL33.GL_INT_2_10_10_10_REV;

public record Accessor(
    @NotNull BufferView bufferView,
    @NotNull ElementType elementType,
    @NotNull ComponentType componentType,
    int offset,
    int count,
    boolean normalized
) {
    public enum ComponentType {
        BYTE(GL_BYTE),
        UNSIGNED_BYTE(GL_UNSIGNED_BYTE),
        SHORT(GL_SHORT),
        UNSIGNED_SHORT(GL_UNSIGNED_SHORT),
        INT(GL_INT),
        UNSIGNED_INT(GL_UNSIGNED_INT),
        HALF_FLOAT(GL_HALF_FLOAT),
        FLOAT(GL_FLOAT),
        INT_10_10_10_2(GL_INT_2_10_10_10_REV),
        UNSIGNED_INT_10_10_10_2(GL_UNSIGNED_INT_10_10_10_2);

        private final int glType;

        ComponentType(int glType) {
            this.glType = glType;
        }

        public int glType() {
            return glType;
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
}

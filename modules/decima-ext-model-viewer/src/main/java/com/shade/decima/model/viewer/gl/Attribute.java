package com.shade.decima.model.viewer.gl;

import com.shade.util.NotNull;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_2_10_10_10_REV;
import static org.lwjgl.opengl.GL30.GL_HALF_FLOAT;
import static org.lwjgl.opengl.GL33.GL_INT_2_10_10_10_REV;

public record Attribute(@NotNull Type type, int components, long offset, int stride, boolean normalized) {
    public enum Type {
        BYTE(GL_BYTE),
        UNSIGNED_BYTE(GL_UNSIGNED_BYTE),
        SHORT(GL_SHORT),
        UNSIGNED_SHORT(GL_UNSIGNED_SHORT),
        INT(GL_INT),
        UNSIGNED_INT(GL_UNSIGNED_INT),
        INT_10_10_10_2(GL_INT_2_10_10_10_REV),
        UNSIGNED_INT_10_10_10_2(GL_UNSIGNED_INT_2_10_10_10_REV),
        HALF_FLOAT(GL_HALF_FLOAT),
        FLOAT(GL_FLOAT);

        private final int glType;

        Type(int glType) {
            this.glType = glType;
        }
    }

    public int glType() {
        return type.glType;
    }

    public int glSize() {
        if (type == Type.INT_10_10_10_2 || type == Type.UNSIGNED_INT_10_10_10_2) {
            return 4;
        } else {
            return components;
        }
    }
}

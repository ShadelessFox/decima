package com.shade.decima.app.gl;

import com.shade.decima.geometry.ComponentType;
import com.shade.decima.geometry.ElementType;
import com.shade.decima.geometry.Semantic;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_2_10_10_10_REV;
import static org.lwjgl.opengl.GL30.GL_HALF_FLOAT;
import static org.lwjgl.opengl.GL33.GL_INT_2_10_10_10_REV;

public record VertexAttribute(
    int location,
    Semantic semantic,
    ElementType elementType,
    ComponentType componentType,
    int offset,
    int stride,
    boolean normalized
) {
    public VertexAttribute {
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

    public int glType() {
        return switch (componentType) {
            case BYTE -> GL_BYTE;
            case UNSIGNED_BYTE -> GL_UNSIGNED_BYTE;
            case SHORT -> GL_SHORT;
            case UNSIGNED_SHORT -> GL_UNSIGNED_SHORT;
            case INT -> GL_INT;
            case UNSIGNED_INT -> GL_UNSIGNED_INT;
            case HALF_FLOAT -> GL_HALF_FLOAT;
            case FLOAT -> GL_FLOAT;
            case INT_10_10_10_2 -> GL_INT_2_10_10_10_REV;
            case UNSIGNED_INT_10_10_10_2 -> GL_UNSIGNED_INT_2_10_10_10_REV;
        };
    }

    public int glSize() {
        return switch (componentType) {
            case INT_10_10_10_2, UNSIGNED_INT_10_10_10_2 -> 4;
            case null, default -> elementType.size();
        };
    }
}

package com.shade.decima.model.viewer.scene;

import com.shade.gl.Attribute.ComponentType;
import com.shade.gl.Attribute.ElementType;
import com.shade.platform.model.util.MathUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.Objects;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;

public record Accessor(
    @NotNull BufferView bufferView,
    @NotNull ElementType elementType,
    @NotNull ComponentType componentType,
    @NotNull Target target,
    int offset,
    int count,
    int stride,
    boolean normalized
) {
    public enum Target {
        VERTEX_ARRAY(GL_ARRAY_BUFFER),
        INDEX_ARRAY(GL_ELEMENT_ARRAY_BUFFER);

        private final int glTarget;

        Target(int glTarget) {
            this.glTarget = glTarget;
        }

        public int glTarget() {
            return glTarget;
        }
    }

    public Accessor(
        @NotNull BufferView bufferView,
        @NotNull ElementType elementType,
        @NotNull ComponentType componentType,
        @NotNull Target target,
        int offset,
        int count,
        boolean normalized
    ) {
        this(bufferView, elementType, componentType, target, offset, count, elementType.componentCount() * componentType.glSize(), normalized);
    }

    public Accessor {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
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

    public int componentCount() {
        return elementType.componentCount();
    }

    public int size() {
        return componentCount() * componentType.glSize();
    }

    @NotNull
    public ByteView asByteView() {
        final ByteBuffer buffer = bufferView.asByteBuffer();

        return switch (componentType) {
            case BYTE, UNSIGNED_BYTE -> ByteView.ofByte(this, buffer);
            default -> throw new UnsupportedOperationException("Unsupported component type: " + componentType);
        };
    }

    @NotNull
    public ShortView asShortView() {
        final ByteBuffer buffer = bufferView.asByteBuffer();

        return switch (componentType) {
            case SHORT, UNSIGNED_SHORT -> ShortView.ofShort(this, buffer);
            default -> throw new UnsupportedOperationException("Unsupported component type: " + componentType);
        };
    }

    @NotNull
    public IntView asIntView() {
        final ByteBuffer buffer = bufferView.asByteBuffer();

        return switch (componentType) {
            case INT, UNSIGNED_INT -> IntView.ofInt(this, buffer);
            default -> throw new UnsupportedOperationException("Unsupported component type: " + componentType);
        };
    }

    @NotNull
    public FloatView asFloatView() {
        final ByteBuffer buffer = bufferView.asByteBuffer();

        return switch (componentType) {
            case FLOAT -> FloatView.ofFloat(this, buffer);
            case HALF_FLOAT -> FloatView.ofHalfFloat(this, buffer);
            case SHORT -> FloatView.ofShort(this, buffer);
            case INT_10_10_10_2 -> FloatView.ofX10Y10Z10W2(this, buffer);
            default -> throw new UnsupportedOperationException("Unsupported component type: " + componentType);
        };
    }

    private int getPositionFor(@NotNull ByteBuffer buffer, int elementIndex, int componentIndex) {
        Objects.checkIndex(elementIndex, count);
        Objects.checkIndex(componentIndex, elementType.componentCount());

        return buffer.position() + offset + (elementIndex * stride) + (componentIndex * componentType.glSize());
    }

    public interface ByteView {
        @NotNull
        static ByteView ofByte(@NotNull Accessor accessor, @NotNull ByteBuffer buffer) {
            return (e, c) -> buffer.get(accessor.getPositionFor(buffer, e, c));
        }

        byte get(int elementIndex, int componentIndex);
    }

    public interface ShortView {
        @NotNull
        static ShortView ofShort(@NotNull Accessor accessor, @NotNull ByteBuffer buffer) {
            return (e, c) -> buffer.getShort(accessor.getPositionFor(buffer, e, c));
        }

        short get(int elementIndex, int componentIndex);
    }

    public interface IntView {
        @NotNull
        static IntView ofInt(@NotNull Accessor accessor, @NotNull ByteBuffer buffer) {
            return (e, c) -> buffer.getInt(accessor.getPositionFor(buffer, e, c));
        }

        int get(int elementIndex, int componentIndex);
    }

    public interface FloatView {
        @NotNull
        static FloatView ofFloat(@NotNull Accessor accessor, @NotNull ByteBuffer buffer) {
            return (e, c) -> buffer.getFloat(accessor.getPositionFor(buffer, e, c));
        }

        @NotNull
        static FloatView ofHalfFloat(@NotNull Accessor accessor, @NotNull ByteBuffer buffer) {
            return (e, c) -> MathUtils.halfToFloat(buffer.getShort(accessor.getPositionFor(buffer, e, c)));
        }

        @NotNull
        static FloatView ofShort(@NotNull Accessor accessor, @NotNull ByteBuffer buffer) {
            return (e, c) -> buffer.getShort(accessor.getPositionFor(buffer, e, c)) / 32767f;
        }

        @NotNull
        static FloatView ofX10Y10Z10W2(@NotNull Accessor accessor, @NotNull ByteBuffer buffer) {
            return (e, c) -> {
                final var value = buffer.getInt(accessor.getPositionFor(buffer, e, 0));

                // Division by 510 gives a smaller error than by 511. How come?
                final var x = (value << 22 >> 22) / 510f;
                final var y = (value << 12 >> 22) / 510f;
                final var z = (value << 2 >> 22) / 510f;
                final var w = (value >> 30);
                final var length = (float) Math.sqrt(x * x + y * y + z * z);

                return switch (c) {
                    case 0 -> x / length;
                    case 1 -> y / length;
                    case 2 -> z / length;
                    case 3 -> w;
                    default -> Objects.checkIndex(c, 4);
                };
            };
        }

        float get(int elementIndex, int componentIndex);
    }
}

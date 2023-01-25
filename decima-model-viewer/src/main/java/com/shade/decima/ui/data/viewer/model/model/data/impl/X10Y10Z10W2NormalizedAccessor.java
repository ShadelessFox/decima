package com.shade.decima.ui.data.viewer.model.model.data.impl;

import com.shade.decima.ui.data.viewer.model.model.data.Accessor;
import com.shade.decima.ui.data.viewer.model.model.data.ComponentType;
import com.shade.decima.ui.data.viewer.model.model.data.ElementType;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class X10Y10Z10W2NormalizedAccessor extends AbstractAccessor {
    public X10Y10Z10W2NormalizedAccessor(@NotNull ByteBuffer buffer, @NotNull ElementType elementType, int elementCount, int offset, int stride) {
        super(buffer, elementType, ComponentType.FLOAT32, elementCount, offset, stride);
    }

    public static float get(@NotNull Accessor src, int elementIndex, int componentIndex) {
        throw new IllegalArgumentException("X10Y10Z10W2NormalizedAccessor does not support reading from other accessors");
    }

    public float get(int elementIndex, int componentIndex) {
        int source = buffer.getInt(getPositionFor(elementIndex, 0));
        int x = (source & 1023 ^ 512) - 512;
        int y = (source >>> 10 & 1023 ^ 512) - 512;
        int z = (source >>> 20 & 1023 ^ 512) - 512;
        int w = (source >>> 30 & 1);

        double length = Math.sqrt(x * x + y * y + z * z);
        return switch (componentIndex) {
            case 0 -> (float) (x / length);
            case 1 -> (float) (y / length);
            case 2 -> (float) (z / length);
            case 3 -> w;
            default -> throw new IllegalStateException("Unexpected value: " + componentIndex);
        };
    }

    public void put(int elementIndex, int componentIndex, float value) {
        throw new IllegalArgumentException("Read only accessor");
    }
}

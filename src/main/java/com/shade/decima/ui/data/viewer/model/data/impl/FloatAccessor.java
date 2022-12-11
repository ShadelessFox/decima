package com.shade.decima.ui.data.viewer.model.data.impl;

import com.shade.decima.ui.data.viewer.model.data.Accessor;
import com.shade.decima.ui.data.viewer.model.data.ComponentType;
import com.shade.decima.ui.data.viewer.model.data.ElementType;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class FloatAccessor extends AbstractAccessor {
    public FloatAccessor(@NotNull ByteBuffer buffer, @NotNull ElementType elementType, int elementCount, int offset, int stride) {
        super(buffer, elementType, ComponentType.FLOAT32, elementCount, offset, stride);
    }

    public FloatAccessor(@NotNull ByteBuffer buffer, @NotNull ElementType elementType, int offset, int stride) {
        super(buffer, elementType, ComponentType.FLOAT32, offset, stride);
    }

    public static float get(@NotNull Accessor src, int elementIndex, int componentIndex) {
        if (src instanceof ByteAccessor acc) {
            return (acc.get(elementIndex, componentIndex) & 0xFF) / 255f;
        } else if (src instanceof ShortAccessor acc) {
            return acc.get(elementIndex, componentIndex) & 0xffff;
        } else if (src instanceof IntAccessor acc) {
            return acc.get(elementIndex, componentIndex);
        } else if (src instanceof FloatAccessor acc) {
            return acc.get(elementIndex, componentIndex);
        } else if (src instanceof HalfFloatAccessor acc) {
            return acc.get(elementIndex, componentIndex);
        } else if (src instanceof X10Y10Z10W2NormalizedAccessor acc) {
            return acc.get(elementIndex, componentIndex);
        } else {
            throw new IllegalArgumentException("Unsupported source accessor: " + src);
        }
    }

    public float get(int elementIndex, int componentIndex) {
        return buffer.getFloat(getPositionFor(elementIndex, componentIndex));
    }

    public void put(int elementIndex, int componentIndex, float value) {
        buffer.putFloat(getPositionFor(elementIndex, componentIndex), value);
    }
}

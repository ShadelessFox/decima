package com.shade.decima.ui.data.viewer.model.data.impl;

import com.shade.decima.ui.data.viewer.model.data.Accessor;
import com.shade.decima.ui.data.viewer.model.data.ComponentType;
import com.shade.decima.ui.data.viewer.model.data.ElementType;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class ByteAccessor extends AbstractAccessor {
    boolean unsigned;

    public ByteAccessor(@NotNull ByteBuffer buffer, @NotNull ElementType elementType, int elementCount, int offset, int stride, boolean unsigned) {
        super(buffer, elementType, ComponentType.INT8, elementCount, offset, stride);
        this.unsigned = unsigned;
    }

    public static byte get(@NotNull Accessor src, int elementIndex, int componentIndex) {
        if (src instanceof ByteAccessor acc) {
            return acc.get(elementIndex, componentIndex);
        } else {
            throw new IllegalArgumentException("Unsupported source accessor: " + src);
        }
    }

    public byte get(int elementIndex, int componentIndex) {
        return buffer.get(getPositionFor(elementIndex, componentIndex));
    }

    public void put(int elementIndex, int componentIndex, byte value) {
        buffer.put(getPositionFor(elementIndex, componentIndex), value);
    }
}

package com.shade.decima.ui.data.viewer.model.data.impl;

import com.shade.decima.ui.data.viewer.model.data.Accessor;
import com.shade.decima.ui.data.viewer.model.data.ComponentType;
import com.shade.decima.ui.data.viewer.model.data.ElementType;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class ShortAccessor extends AbstractAccessor {
    boolean unsigned;

    public ShortAccessor(@NotNull ByteBuffer buffer, @NotNull ElementType elementType, int elementCount, int offset, int stride, boolean unsigned) {
        super(buffer, elementType, ComponentType.INT16, elementCount, offset, stride);
        this.unsigned = unsigned;
    }

    public static short get(@NotNull Accessor src, int elementIndex, int componentIndex) {
        if (src instanceof ByteAccessor acc) {
            return (short) (acc.get(elementIndex, componentIndex) & 0xff);
        } else if (src instanceof ShortAccessor acc) {
            return acc.get(elementIndex, componentIndex);
        } else {
            throw new IllegalArgumentException("Unsupported source accessor: " + src);
        }
    }

    public short get(int elementIndex, int componentIndex) {
        return buffer.getShort(getPositionFor(elementIndex, componentIndex));
    }

    public void put(int elementIndex, int componentIndex, short value) {
        buffer.putShort(getPositionFor(elementIndex, componentIndex), value);
    }
}

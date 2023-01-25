package com.shade.decima.ui.data.viewer.model.model.data.impl;

import com.shade.decima.ui.data.viewer.model.model.data.Accessor;
import com.shade.decima.ui.data.viewer.model.model.data.ComponentType;
import com.shade.decima.ui.data.viewer.model.model.data.ElementType;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class IntAccessor extends AbstractAccessor {
    boolean unsigned;

    public IntAccessor(@NotNull ByteBuffer buffer, @NotNull ElementType elementType, int elementCount, int offset, int stride, boolean unsigned) {
        super(buffer, elementType, ComponentType.INT16, elementCount, offset, stride);
        this.unsigned = unsigned;
    }

    public static int get(@NotNull Accessor src, int elementIndex, int componentIndex) {
        if (src instanceof ByteAccessor acc) {
            return acc.get(elementIndex, componentIndex) & 0xff;
        } else if (src instanceof ShortAccessor acc) {
            return acc.get(elementIndex, componentIndex) & 0xffff;
        } else if (src instanceof IntAccessor acc) {
            return acc.get(elementIndex, componentIndex);
        } else {
            throw new IllegalArgumentException("Unsupported source accessor: " + src);
        }
    }

    public int get(int elementIndex, int componentIndex) {

        return buffer.getInt(getPositionFor(elementIndex, componentIndex));
    }

    public void put(int elementIndex, int componentIndex, int value) {
        buffer.putInt(getPositionFor(elementIndex, componentIndex), value);
    }
}

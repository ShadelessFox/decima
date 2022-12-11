package com.shade.decima.ui.data.viewer.model.data.impl;

import com.shade.decima.ui.data.viewer.model.data.Accessor;
import com.shade.decima.ui.data.viewer.model.data.ComponentType;
import com.shade.decima.ui.data.viewer.model.data.ElementType;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class HalfFloatAccessor extends AbstractAccessor {
    public HalfFloatAccessor(@NotNull ByteBuffer buffer, @NotNull ElementType elementType, int elementCount, int offset, int stride) {
        super(buffer, elementType, ComponentType.UINT16, elementCount, offset, stride);
    }

    public static float get(@NotNull Accessor src, int elementIndex, int componentIndex) {
        throw new IllegalArgumentException("HalfFloatAccessor does not support reading from other accessors");
    }

    public float get(int elementIndex, int componentIndex) {
        return IOUtils.halfToFloat(buffer.getShort(getPositionFor(elementIndex, componentIndex)));
    }

    public void put(int elementIndex, int componentIndex, float value) {
        throw new IllegalArgumentException("Read only accessor");
    }
}

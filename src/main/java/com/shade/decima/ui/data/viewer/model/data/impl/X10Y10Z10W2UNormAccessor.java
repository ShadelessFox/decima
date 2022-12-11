package com.shade.decima.ui.data.viewer.model.data.impl;

import com.shade.decima.ui.data.viewer.model.data.Accessor;
import com.shade.decima.ui.data.viewer.model.data.ComponentType;
import com.shade.decima.ui.data.viewer.model.data.ElementType;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class X10Y10Z10W2UNormAccessor extends AbstractAccessor {
    public X10Y10Z10W2UNormAccessor(@NotNull ByteBuffer buffer, @NotNull ElementType elementType, int elementCount, int offset, int stride) {
        super(buffer, elementType, ComponentType.INT16, elementCount, offset, stride);
    }

    public static float get(@NotNull Accessor src, int elementIndex, int componentIndex) {
        throw new IllegalArgumentException("X10Y10Z10W2UNormAccessor does not support reading from other accessors");
    }

    public float get(int elementIndex, int componentIndex) {
        throw new IllegalArgumentException("X10Y10Z10W2UNormAccessor does not support reading from buffer");
    }

    public void put(int elementIndex, int componentIndex, float value) {
        throw new IllegalArgumentException("Read only accessor");
    }
}

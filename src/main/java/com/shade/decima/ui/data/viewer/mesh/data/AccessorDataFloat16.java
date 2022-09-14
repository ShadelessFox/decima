package com.shade.decima.ui.data.viewer.mesh.data;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

import com.shade.decima.ui.data.viewer.mesh.utils.MathUtils;

public class AccessorDataFloat16 extends AccessorDataAbstract {
    public AccessorDataFloat16(@NotNull ByteBuffer buffer, @NotNull ElementType elementType, int elementCount, int componentCount, int stride, int offset) {
        super(buffer, elementType, ComponentType.UNSIGNED_SHORT, elementCount, componentCount, stride, offset);
    }

    public float get(int elementIndex, int componentIndex) {
        return MathUtils.toFloat(getBuffer().getShort(getPosition(elementIndex, componentIndex)));
    }

    public void put(int elementIndex, int componentIndex, float value) {
        throw new IllegalStateException("Not implemented");
    }
}

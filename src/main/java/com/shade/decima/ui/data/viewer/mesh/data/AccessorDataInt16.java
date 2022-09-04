package com.shade.decima.ui.data.viewer.mesh.data;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class AccessorDataInt16 extends AccessorDataAbstract {
    protected final boolean normalized;

    public AccessorDataInt16(@NotNull ByteBuffer buffer, @NotNull ElementType elementType, int elementCount, int componentCount, int stride, int offset, boolean unsigned, boolean normalized) {
        super(buffer, elementType, unsigned ? ComponentType.UNSIGNED_SHORT : ComponentType.SHORT, elementCount, componentCount, stride, offset);
        this.normalized = normalized;
    }

    public short get(int elementIndex, int componentIndex) {
        return getBuffer().getShort(getPosition(elementIndex, componentIndex));
    }

    public void put(int elementIndex, int componentIndex, short value) {
        getBuffer().putShort(getPosition(elementIndex, componentIndex), value);
    }

    public boolean isNormalized() {
        return normalized;
    }
}

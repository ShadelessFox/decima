package com.shade.decima.ui.data.viewer.mesh.data;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class AccessorDataInt8 extends AccessorDataAbstract {
    protected final boolean normalized;

    public AccessorDataInt8(@NotNull ByteBuffer buffer, @NotNull ElementType elementType, int elementCount, int componentCount, int stride, int offset, boolean unsigned, boolean normalized) {
        super(buffer, elementType, unsigned ? ComponentType.UNSIGNED_BYTE : ComponentType.BYTE, elementCount, componentCount, stride, offset);
        this.normalized = normalized;
    }

    public byte get(int elementIndex, int componentIndex) {
        return getBuffer().get(getPosition(elementIndex, componentIndex));
    }

    public void put(int elementIndex, int componentIndex, byte value) {
        getBuffer().put(getPosition(elementIndex, componentIndex), value);
    }

    public boolean isNormalized() {
        return normalized;
    }
}

package com.shade.decima.ui.data.viewer.mesh.data;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class AccessorDataInt32 extends AccessorDataAbstract {
    protected final boolean normalized;

    public AccessorDataInt32(@NotNull ByteBuffer buffer, @NotNull ElementType elementType, int elementCount, int componentCount, int stride, int offset, boolean unsigned, boolean normalized) {
        super(buffer, elementType, unsigned ? ComponentType.UNSIGNED_INT : ComponentType.INT, elementCount, componentCount, stride, offset);
        this.normalized = normalized;
    }

    public int get(int elementIndex, int componentIndex) {
        return getBuffer().getInt(getPosition(elementIndex, componentIndex));
    }

    public void put(int elementIndex, int componentIndex, int value) {
        getBuffer().putInt(getPosition(elementIndex, componentIndex), value);
    }

    public boolean isNormalized() {
        return normalized;
    }
}

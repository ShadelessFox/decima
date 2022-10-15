package com.shade.decima.ui.data.viewer.mesh.data;

import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class AccessorDataXYZ10W2 extends AccessorDataAbstract {
    protected final boolean unsigned;
    protected final boolean normalized;

    public AccessorDataXYZ10W2(@NotNull ByteBuffer buffer, @NotNull ElementType elementType, int elementCount, int componentCount, int stride, int offset, boolean unsigned, boolean normalized) {
        super(buffer, elementType, unsigned ? ComponentType.UNSIGNED_SHORT : ComponentType.SHORT, elementCount, componentCount, stride, offset);
        this.unsigned = unsigned;
        this.normalized = normalized;
    }

    public short get(int elementIndex, int componentIndex) {
        final int data = getBuffer().getInt(getPosition(elementIndex, componentIndex));

        return switch (componentIndex) {
            case 0 -> IOUtils.signExtend(data >>> 22 & 1023, 10);
            case 1 -> IOUtils.signExtend(data >>> 12 & 1023, 10);
            case 2 -> IOUtils.signExtend(data >>> 2 & 1023, 10);
            default -> (short) (data & 3);
        };
    }

    public void put(int elementIndex, int componentIndex, short value) {
        throw new IllegalStateException("Not implemented");
    }

    public boolean isNormalized() {
        return normalized;
    }
}
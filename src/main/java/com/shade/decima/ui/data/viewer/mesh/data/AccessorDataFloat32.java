package com.shade.decima.ui.data.viewer.mesh.data;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class AccessorDataFloat32 extends AccessorDataAbstract {
    public AccessorDataFloat32(@NotNull ByteBuffer buffer, @NotNull ElementType elementType, int elementCount, int componentCount, int stride, int offset) {
        super(buffer, elementType, ComponentType.FLOAT, elementCount, componentCount, stride, offset);
    }

    public float get(int elementIndex, int componentIndex) {
        return getBuffer().getFloat(getPosition(elementIndex, componentIndex));
    }

    public void put(int elementIndex, int componentIndex, float value) {
        getBuffer().putFloat(getPosition(elementIndex, componentIndex), value);
    }
}

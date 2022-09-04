package com.shade.decima.ui.data.viewer.mesh.data;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.Objects;

public abstract class AccessorDataAbstract implements AccessorData {
    private final ByteBuffer buffer;
    private final ElementType elementType;
    private final ComponentType componentType;
    private final int elementCount;
    private final int componentCount;
    private final int stride;
    private final int offset;

    public AccessorDataAbstract(@NotNull ByteBuffer buffer, @NotNull ElementType elementType, @NotNull ComponentType componentType, int elementCount, int componentCount, int stride, int offset) {
        this.buffer = buffer;
        this.elementType = elementType;
        this.componentType = componentType;
        this.elementCount = elementCount;
        this.componentCount = componentCount > 0 ? componentCount : elementType.getComponentCount();
        this.stride = stride > 0 ? stride : elementType.getStride(componentType);
        this.offset = offset;
    }

    @NotNull
    @Override
    public ElementType getElementType() {
        return elementType;
    }

    @NotNull
    @Override
    public ComponentType getComponentType() {
        return componentType;
    }

    @Override
    public int getElementCount() {
        return elementCount;
    }

    @Override
    public int getComponentCount() {
        return componentCount;
    }

    @NotNull
    protected ByteBuffer getBuffer() {
        return buffer;
    }

    protected int getPosition(int elementIndex, int componentIndex) {
        Objects.checkIndex(elementIndex, elementCount);
        Objects.checkIndex(componentIndex, componentCount);

        return offset + elementIndex * stride + componentIndex * componentType.getSize();
    }
}

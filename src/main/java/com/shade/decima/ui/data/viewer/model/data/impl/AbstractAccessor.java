package com.shade.decima.ui.data.viewer.model.data.impl;

import com.shade.decima.ui.data.viewer.model.data.Accessor;
import com.shade.decima.ui.data.viewer.model.data.ComponentType;
import com.shade.decima.ui.data.viewer.model.data.ElementType;
import com.shade.util.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.util.Objects;

public abstract class AbstractAccessor implements Accessor {
    protected final ByteBuffer buffer;
    private final ElementType elementType;
    private final ComponentType componentType;
    private final int elementCount;
    private final int componentCount;
    private final int offset;
    private final int stride;

    public AbstractAccessor(@NotNull ByteBuffer buffer, @NotNull ElementType elementType, @NotNull ComponentType componentType, int elementCount, int offset, int stride) {
        this.buffer = buffer;
        this.elementType = elementType;
        this.componentType = componentType;
        this.elementCount = elementCount;
        this.componentCount = elementType.getComponentCount();
        this.offset = offset;
        this.stride = stride;
    }

    public AbstractAccessor(@NotNull ByteBuffer buffer, @NotNull ElementType elementType, @NotNull ComponentType componentType, int offset, int stride) {
        this(buffer, elementType, componentType, buffer.remaining() / stride, offset, stride);
    }

    public static void transfer(@NotNull Accessor src, @NotNull Accessor dst) throws Throwable {
        if (src.getElementCount() != dst.getElementCount()) {
            throw new IllegalArgumentException("dst and src length does not match");
        }

        if (dst.getComponentCount() > src.getComponentCount()) {
            throw new IllegalArgumentException("Cannot transfer from src to dst due to src having more components than dst");
        }

        final Class<?> type = dst.getComponentType().getType();
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        final MethodHandle get = lookup.findStatic(dst.getClass(), "get", MethodType.methodType(type, new Class[]{Accessor.class, int.class, int.class}));
        final MethodHandle put = lookup.findVirtual(dst.getClass(), "put", MethodType.methodType(void.class, new Class[]{int.class, int.class, type}));

        for (int elem = 0; elem < dst.getElementCount(); elem++) {
            for (int comp = 0; comp < dst.getComponentCount(); comp++) {
                put.invoke(dst, elem, comp, get.invoke(src, elem, comp));
            }
        }
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

    protected int getPositionFor(int elementIndex, int componentIndex) {
        Objects.checkIndex(elementIndex, elementCount);
        Objects.checkIndex(componentIndex, componentCount);

        return offset + elementIndex * stride + componentIndex * componentType.getSize();
    }
}

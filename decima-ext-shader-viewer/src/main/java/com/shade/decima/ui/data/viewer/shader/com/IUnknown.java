package com.shade.decima.ui.data.viewer.shader.com;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public class IUnknown implements AutoCloseable {
    protected final MemorySegment segment;

    private final MethodHandle Release;

    public IUnknown(MemorySegment segment) {
        this.segment = segment.reinterpret(ADDRESS.byteSize());

        Release = downcallHandle(2, FunctionDescriptor.of(JAVA_INT, ADDRESS));
    }

    @Override
    public void close() {
        try {
            Release.invoke(segment);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    protected MethodHandle downcallHandle(int vtableIndex, FunctionDescriptor descriptor) {
        var vtable = segment.get(ADDRESS, 0);
        var address = vtable
            .reinterpret(ADDRESS.byteSize() * (vtableIndex + 1))
            .get(ADDRESS, ADDRESS.byteSize() * vtableIndex);
        return Linker.nativeLinker()
            .downcallHandle(address, descriptor);
    }
}

package com.shade.decima.ui.data.viewer.shader.ffm;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public class IUnknown implements AutoCloseable {
    protected final MemorySegment segment;

    private final MethodHandle QueryInterface;
    private final MethodHandle AddRef;
    private final MethodHandle Release;

    public IUnknown(MemorySegment segment) {
        this.segment = segment.reinterpret(ADDRESS.byteSize());

        QueryInterface = downcallHandle(0, FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
        AddRef = downcallHandle(1, FunctionDescriptor.of(JAVA_INT, ADDRESS));
        Release = downcallHandle(2, FunctionDescriptor.of(JAVA_INT, ADDRESS));
    }

    public <T> T queryInterface(IID<T> iid) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment riid = arena.allocate(GUID.BYTES);
            iid.guid().set(riid, 0);

            MemorySegment ppvObject = arena.allocate(ADDRESS);
            queryInterface(riid, ppvObject);

            return iid.constructor().apply(ppvObject.get(ADDRESS, 0));
        }
    }

    private void queryInterface(MemorySegment riid, MemorySegment ppvObject) {
        try {
            COMException.check((int) QueryInterface.invokeExact(segment, riid, ppvObject));
        } catch (COMException e) {
            throw e;
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
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

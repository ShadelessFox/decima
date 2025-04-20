package com.shade.decima.ui.data.viewer.shader.com;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

public final class IDxcUtils extends IUnknown {
    public static final IID<IDxcUtils> IID_IDxcUtils = IID.of("4605c4cb-2019-492a-ada4-65f20bb7d67f", IDxcUtils::new);

    private final MethodHandle CreateBlob;

    public IDxcUtils(MemorySegment segment) {
        super(segment);

        CreateBlob = downcallHandle(6, FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS));
    }

    public IDxcBlob createBlob(byte[] buffer, int codePage) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment data = arena.allocateFrom(JAVA_BYTE, buffer);
            MemorySegment blob = arena.allocate(ADDRESS.byteSize());
            createBlob(data, buffer.length, codePage, blob);
            return new IDxcBlob(blob.get(ADDRESS, 0));
        }
    }

    private void createBlob(MemorySegment pData, int size, int codePage, MemorySegment pBlobEncoding) {
        try {
            COMException.check((int) CreateBlob.invokeExact(segment, pData, size, codePage, pBlobEncoding));
        } catch (COMException e) {
            throw e;
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }
}

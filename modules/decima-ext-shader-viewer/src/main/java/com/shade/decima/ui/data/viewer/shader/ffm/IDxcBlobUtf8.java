package com.shade.decima.ui.data.viewer.shader.ffm;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

public class IDxcBlobUtf8 extends IDxcBlobEncoding {
    private final MethodHandle GetStringPointer;
    private final MethodHandle GetStringLength;

    public IDxcBlobUtf8(MemorySegment segment) {
        super(segment);

        GetStringPointer = downcallHandle(6, FunctionDescriptor.of(ADDRESS, ADDRESS));
        GetStringLength = downcallHandle(7, FunctionDescriptor.of(JAVA_LONG, ADDRESS));
    }

    public String getString() {
        var pointer = getStringPointer();
        var length = getBufferSize();
        return pointer.reinterpret(length).getString(0, StandardCharsets.UTF_8);
    }

    public MemorySegment getStringPointer() {
        try {
            return (MemorySegment) GetStringPointer.invokeExact(segment);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    public long getStringLength() {
        try {
            return (long) GetStringLength.invokeExact(segment);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }
}

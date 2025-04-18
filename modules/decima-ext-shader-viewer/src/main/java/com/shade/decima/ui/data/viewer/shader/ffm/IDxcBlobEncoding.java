package com.shade.decima.ui.data.viewer.shader.ffm;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public class IDxcBlobEncoding extends IDxcBlob {
    private final MethodHandle GetEncoding;

    public IDxcBlobEncoding(MemorySegment segment) {
        super(segment);

        GetEncoding = downcallHandle(5, FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
    }

    public void getEncoding(MemorySegment pKnown, MemorySegment pCodePage) {
        try {
            COMException.check((int) GetEncoding.invokeExact(segment, pKnown, pCodePage));
        } catch (COMException e) {
            throw e;
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }
}

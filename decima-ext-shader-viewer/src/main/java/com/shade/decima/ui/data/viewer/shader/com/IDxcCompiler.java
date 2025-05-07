package com.shade.decima.ui.data.viewer.shader.com;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public final class IDxcCompiler extends IUnknown {
    public static final IID<IDxcCompiler> IID_IDxcCompiler = IID.of("8c210bf3-011f-4422-8d70-6f9acb8db617", IDxcCompiler::new);

    private final MethodHandle Disassemble;

    private IDxcCompiler(MemorySegment segment) {
        super(segment);

        Disassemble = downcallHandle(5, FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
    }

    public IDxcBlob disassemble(IDxcBlob source) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment disassembly = arena.allocate(ADDRESS.byteSize());
            disassemble(source.segment, disassembly);
            return new IDxcBlob(disassembly.get(ADDRESS, 0));
        }
    }

    private void disassemble(MemorySegment pSource, MemorySegment ppDisassembly) {
        try {
            COMException.check((int) Disassemble.invokeExact(segment, pSource, ppDisassembly));
        } catch (COMException e) {
            throw e;
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }
}

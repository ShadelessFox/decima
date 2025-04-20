package com.shade.decima.ui.data.viewer.shader.com;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

public final class D3DCompiler {
    private final MethodHandle D3DDisassemble;

    public D3DCompiler(SymbolLookup lookup) {
        Linker linker = Linker.nativeLinker();

        D3DDisassemble = linker.downcallHandle(
            lookup.findOrThrow("D3DDisassemble"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS)
        );
    }

    public IDxcBlob disassemble(byte[] source, int flags, String comments) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pSrcData = arena.allocateFrom(JAVA_BYTE, source);
            MemorySegment szComments = comments != null ? arena.allocateFrom(comments) : MemorySegment.NULL;
            MemorySegment ppDisassembly = arena.allocate(ADDRESS);
            disassemble(pSrcData, source.length, flags, szComments, ppDisassembly);
            return new IDxcBlob(ppDisassembly.get(ADDRESS, 0));
        }
    }

    private void disassemble(MemorySegment pSrcData, int srcDataSize, int flags, MemorySegment szComments, MemorySegment ppDisassembly) {
        try {
            COMException.check((int) D3DDisassemble.invokeExact(pSrcData, srcDataSize, flags, szComments, ppDisassembly));
        } catch (COMException e) {
            throw e;
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }
}

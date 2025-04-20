package com.shade.decima.ui.data.viewer.shader.com;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public final class DXCompiler {
    public static final GUID CLSID_DxcUtils = GUID.of("6245d6af-66e0-48fd-80b4-4d271796748c");
    public static final GUID CLSID_DxcCompiler = GUID.of("73e22d93-e6ce-47f3-b5bf-f0664f39c1b0");

    private final MethodHandle DxcCreateInstance;

    public DXCompiler(SymbolLookup lookup) {
        Linker linker = Linker.nativeLinker();

        DxcCreateInstance = linker.downcallHandle(
            lookup.findOrThrow("DxcCreateInstance"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS)
        );
    }

    public <T> T createInstance(GUID clsid, IID<T> iid) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment rclsid = arena.allocate(GUID.BYTES);
            clsid.set(rclsid, 0);

            MemorySegment riid = arena.allocate(GUID.BYTES);
            iid.guid().set(riid, 0);

            MemorySegment ppv = arena.allocate(ADDRESS);
            createInstance(rclsid, riid, ppv);

            return iid.constructor().apply(ppv.get(ADDRESS, 0));
        }
    }

    private void createInstance(MemorySegment rclsid, MemorySegment riid, MemorySegment ppv) {
        try {
            COMException.check((int) DxcCreateInstance.invokeExact(rclsid, riid, ppv));
        } catch (COMException e) {
            throw e;
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }
}

package com.shade.decima.ui.data.viewer.shader.com;

import com.shade.util.NotNull;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

public class IDxcCompiler extends IUnknown {
    public static final GUID CLSID_DxcCompiler = new GUID("73e22d93-e6ce-47f3-b5bf-f0664f39c1b0");
    public static final GUID IID_IDxcCompiler = new GUID("8c210bf3-011f-4422-8d70-6f9acb8db617");

    public IDxcCompiler(@NotNull Pointer p) {
        super(p);
    }

    public void Disassemble(@NotNull IDxcBlob source, @NotNull IDxcBlobEncoding disassembly) {
        final PointerByReference disassemblyRef = new PointerByReference();

        try {
            invokeResult(5, getPointer(), source, disassemblyRef);
        } finally {
            disassembly.setPointer(disassemblyRef.getValue());
        }
    }
}

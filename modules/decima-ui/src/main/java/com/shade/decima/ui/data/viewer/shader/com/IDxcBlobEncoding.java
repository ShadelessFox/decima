package com.shade.decima.ui.data.viewer.shader.com;

import com.shade.util.NotNull;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.IntByReference;

public class IDxcBlobEncoding extends IDxcBlob {
    public IDxcBlobEncoding(@NotNull Pointer p) {
        super(p);
    }

    public IDxcBlobEncoding() {
        super();
    }

    public void GetEncoding(@NotNull ByteByReference pKnown, @NotNull IntByReference pCodePage) {
        invokeResult(5, getPointer(), pKnown, pCodePage);
    }
}

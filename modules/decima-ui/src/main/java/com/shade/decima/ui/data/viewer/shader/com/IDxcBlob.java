package com.shade.decima.ui.data.viewer.shader.com;

import com.shade.util.NotNull;
import com.sun.jna.Pointer;

import java.nio.charset.StandardCharsets;

public class IDxcBlob extends IUnknown {
    public IDxcBlob(@NotNull Pointer p) {
        super(p);
    }

    public IDxcBlob() {
        super();
    }

    public void get(@NotNull byte[] dst, int offset, int length) {
        getBufferPointer().read(0, dst, offset, length);
    }

    @NotNull
    public String getString() {
        return getBufferPointer().getString(0, StandardCharsets.UTF_8.name());
    }

    @NotNull
    public Pointer getBufferPointer() {
        return invoke(3, Pointer.class, getPointer());
    }

    public int getBufferSize() {
        return invokeInt(4, getPointer());
    }
}

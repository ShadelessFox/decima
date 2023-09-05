package com.shade.decima.ui.data.viewer.shader.com;

import com.shade.util.NotNull;
import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.PointerByReference;

import java.util.UUID;

public class IUnknown extends PointerType {
    public IUnknown(@NotNull Pointer p) {
        super(p);
    }

    public IUnknown() {
    }

    public void QueryInterface(@NotNull UUID riid, @NotNull PointerByReference ppvObject) {
        invokeResult(0, getPointer(), riid, ppvObject);
    }

    public int AddRef() {
        return invokeInt(1, getPointer());
    }

    public int Release() {
        return invokeInt(2, getPointer());
    }

    @NotNull
    protected <T> T invoke(int vtableId, @NotNull Class<T> returnType, @NotNull Object... args) {
        final Pointer vtbl = getPointer().getPointer(0);
        final Function func = Function.getFunction(vtbl.getPointer((long) vtableId * Native.POINTER_SIZE));
        return returnType.cast(func.invoke(returnType, args));
    }

    protected int invokeInt(int vtableId, @NotNull Object... args) {
        final Pointer vtbl = getPointer().getPointer(0);
        final Function func = Function.getFunction(vtbl.getPointer((long) vtableId * Native.POINTER_SIZE));
        return func.invokeInt(args);
    }

    protected void invokeVoid(int vtableId, @NotNull Object... args) {
        final Pointer vtbl = getPointer().getPointer(0);
        final Function func = Function.getFunction(vtbl.getPointer((long) vtableId * Native.POINTER_SIZE));
        func.invokeVoid(args);
    }

    protected void invokeResult(int vtableId, @NotNull Object... args) {
        final int rc = invokeInt(vtableId, args);

        if (rc < 0) {
            throw new IllegalStateException("Error: %#10x".formatted(rc));
        }
    }
}

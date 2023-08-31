package com.shade.decima.ui.data.viewer.shader.com;

import com.shade.util.NotNull;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

public class IDxcUtils extends IUnknown {
    public static final GUID CLSID_DxcUtils = new GUID("6245d6af-66e0-48fd-80b4-4d271796748c");
    public static final GUID IID_IDxcUtils = new GUID("4605c4cb-2019-492a-ada4-65f20bb7d67f");

    public IDxcUtils(@NotNull Pointer p) {
        super(p);
    }

    public void CreateBlob(@NotNull byte[] data, int codePage, IDxcBlobEncoding pBlobEncoding) {
        final PointerByReference pBlobEncodingRef = new PointerByReference();

        try {
            invokeResult(6, getPointer(), data, data.length, codePage, pBlobEncodingRef);
        } finally {
            pBlobEncoding.setPointer(pBlobEncodingRef.getValue());
        }
    }
}

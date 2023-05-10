package com.shade.decima.ui.controls.hex;

import com.shade.util.NotNull;

import java.awt.datatransfer.*;
import java.io.ByteArrayInputStream;

public record ByteContents(@NotNull byte[] data) implements Transferable, ClipboardOwner {
    private static final DataFlavor byteArrayInputStream = getByteArrayInputStream();
    private static final DataFlavor[] flavors = {byteArrayInputStream};

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return byteArrayInputStream.equals(flavor);
    }

    @NotNull
    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (byteArrayInputStream.equals(flavor)) {
            return new ByteArrayInputStream(data);
        }

        throw new UnsupportedFlavorException(flavor);
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        // we don't care
    }

    @NotNull
    private static DataFlavor getByteArrayInputStream() {
        try {
            return new DataFlavor("application/octet-stream; class=java.io.ByteArrayInputStream");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Error constructing flavor", e);
        }
    }
}

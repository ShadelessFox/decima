package com.shade.decima.ui.data.viewer.model.dmf;

import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.Base64;

public class DMFInternalBuffer extends DMFBuffer {
    public String bufferData;

    public DMFInternalBuffer(@NotNull ByteBuffer src) {
        final byte[] data = IOUtils.getBytesExact(src, src.remaining());
        bufferSize = data.length;
        bufferData = Base64.getEncoder().encodeToString(data);
    }
}

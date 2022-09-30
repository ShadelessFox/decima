package com.shade.decima.ui.data.viewer.mesh.dmf;

import com.shade.platform.model.util.IOUtils;

import java.nio.ByteBuffer;
import java.util.Base64;

public class DMFInternalBuffer extends DMFBuffer {
    public String bufferData;


    public DMFInternalBuffer(ByteBuffer src) {
        bufferSize = src.remaining();
        bufferData = Base64.getEncoder().encodeToString(IOUtils.getBytesExact(src, src.remaining()));
    }
}

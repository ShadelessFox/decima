package com.shade.decima.ui.data.viewer.mesh.dmf;

import com.shade.platform.model.util.IOUtils;

import javax.crypto.ShortBufferException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;

public class DMFInternalBuffer extends DMFBuffer {
    public String bufferData;


    public DMFInternalBuffer(ByteBuffer src) throws IOException {
        bufferSize = src.remaining();
        byte[] data = IOUtils.getBytesExact(src, src.remaining());
        if (data.length != bufferSize) {
            throw new IOException();
        }
        bufferData = Base64.getEncoder().encodeToString(data);
    }
}

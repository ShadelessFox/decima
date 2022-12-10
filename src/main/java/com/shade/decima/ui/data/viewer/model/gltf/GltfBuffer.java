package com.shade.decima.ui.data.viewer.model.gltf;

import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.Base64;

public class GltfBuffer {
    public String name;
    public String uri;
    public int byteLength;

    public GltfBuffer(@NotNull GltfFile file) {
        file.buffers.add(this);
    }

    public void setData(ByteBuffer src) {
        byteLength = src.remaining();
        uri = "data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(IOUtils.getBytesExact(src, src.remaining()));
    }
}

package com.shade.decima.ui.data.viewer.model.gltf;

import com.shade.util.NotNull;

public class GltfBufferView {
    public String name;
    public int buffer;
    public int byteOffset;
    public int byteLength;
    public Integer byteStride;

    public GltfBufferView(@NotNull GltfFile file, @NotNull GltfBuffer buffer) {
        this.buffer = file.buffers.indexOf(buffer);
        file.bufferViews.add(this);
    }
}

package com.shade.decima.ui.data.viewer.mesh.gltf;

import com.shade.util.NotNull;

public class GltfBuffer {
    public String name;
    public String uri;
    public int byteLength;

    public GltfBuffer(@NotNull GltfFile file) {
        file.buffers.add(this);
    }
}

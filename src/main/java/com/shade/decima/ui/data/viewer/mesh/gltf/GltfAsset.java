package com.shade.decima.ui.data.viewer.mesh.gltf;

import com.shade.util.NotNull;

public class GltfAsset {
    public String generator;
    public String version;

    public GltfAsset(@NotNull GltfFile file) {
        file.asset = this;
    }
}

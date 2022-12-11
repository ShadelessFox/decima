package com.shade.decima.ui.data.viewer.model.gltf;

import com.shade.util.NotNull;

public class GltfAsset {
    public String generator;
    public String version;

    public GltfAsset(@NotNull String generator, @NotNull String version) {
        this.generator = generator;
        this.version = version;
    }
}

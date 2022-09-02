package com.shade.decima.ui.data.viewer.mesh.gltf;

import com.shade.util.NotNull;

public class GltfAccessor {
    public int bufferView;
    public int componentType;
    public int count;
    public String type;

    public GltfAccessor(@NotNull GltfFile file, @NotNull GltfBufferView view) {
        this.bufferView = file.bufferViews.indexOf(view);
        file.accessors.add(this);
    }
}

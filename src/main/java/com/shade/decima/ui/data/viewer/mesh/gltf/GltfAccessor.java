package com.shade.decima.ui.data.viewer.mesh.gltf;

import com.shade.util.NotNull;

public class GltfAccessor {
    public String name;
    public int bufferView;
    public int componentType;
    public int count;
    public String type;
    public boolean normalized;
    public double[] min;
    public double[] max;

    public GltfAccessor(@NotNull GltfFile file, @NotNull GltfBufferView view) {
        this.bufferView = file.bufferViews.indexOf(view);
        file.accessors.add(this);
    }
}

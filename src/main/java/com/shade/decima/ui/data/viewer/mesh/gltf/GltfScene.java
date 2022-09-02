package com.shade.decima.ui.data.viewer.mesh.gltf;

import com.shade.util.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GltfScene {
    public final List<Integer> nodes = new ArrayList<>();

    public GltfScene(@NotNull GltfFile file) {
        file.scenes.add(this);
    }
}

package com.shade.decima.ui.data.viewer.mesh.gltf;

import com.shade.util.NotNull;

public class GltfNode {
    public int mesh;
    public double[] matrix;

    public GltfNode(@NotNull GltfFile file, @NotNull GltfScene scene, @NotNull GltfMesh mesh) {
        this.mesh = file.meshes.indexOf(mesh);
        file.nodes.add(this);
        scene.nodes.add(file.nodes.indexOf(this));
    }
}

package com.shade.decima.ui.data.viewer.mesh.gltf;

import com.shade.decima.ui.data.viewer.mesh.utils.Matrix4x4;
import com.shade.util.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GltfNode {
    public transient boolean isTransformed = false;
    public transient Matrix4x4 parentMatrix;
    public String name;
    public double[] rotation;
    public double[] scale;
    public double[] translation;
    List<Integer> children = new ArrayList<>();
    public Integer mesh;
    public Integer skin;

    public GltfNode(@NotNull GltfFile file, @NotNull GltfMesh mesh) {
        this.mesh = file.meshes.indexOf(mesh);
        file.nodes.add(this);
    }

    public GltfNode(@NotNull GltfFile file) {
        file.nodes.add(this);
    }

    public GltfNode(@NotNull GltfFile file, String name) {
        file.nodes.add(this);
        this.name = name;
    }

    public void addNode(@NotNull GltfNode child, @NotNull GltfFile file) {
        assert file.nodes.contains(child);
        children.add(file.nodes.indexOf(child));
    }

    public void setSkin(GltfSkin skin, GltfFile file) {
        this.skin = file.skins.indexOf(skin);
    }
}

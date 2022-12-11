package com.shade.decima.ui.data.viewer.model.gltf;

import com.shade.decima.ui.data.viewer.model.utils.Matrix4x4;
import com.shade.decima.ui.data.viewer.model.utils.Transform;

import java.util.ArrayList;
import java.util.List;

public class GltfNode {
    public transient boolean isTransformed = false;
    public transient Matrix4x4 parentMatrix;
    public String name;
    public double[] rotation;
    public double[] scale;
    public double[] translation;
    public List<Integer> children = new ArrayList<>();
    public Integer mesh;
    public Integer skin;

    GltfNode() {
    }

    GltfNode(int mesh) {
        this.mesh = mesh;
    }

    GltfNode(String name) {
        this.name = name;
    }

    GltfNode(String name, int mesh) {
        this.name = name;
        this.mesh = mesh;
    }

    public void setTransform(Transform transform) {
        rotation = transform.getRotation();
        translation = transform.getTranslation();
        scale = transform.getScale();
    }

//    public void addNode(@NotNull GltfNode child, @NotNull GltfFile file) {
//        assert file.nodes.contains(child);
//        children.add(file.nodes.indexOf(child));
//    }
//
//    public void setSkin(GltfSkin skin, GltfFile file) {
//        this.skin = file.skins.indexOf(skin);
//    }
}

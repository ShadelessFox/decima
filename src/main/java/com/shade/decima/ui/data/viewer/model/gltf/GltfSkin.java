package com.shade.decima.ui.data.viewer.model.gltf;

import java.util.ArrayList;
import java.util.List;

public class GltfSkin {
    public String name;
    public Integer inverseBindMatrices;
    public Integer skeleton;
    public List<Integer> joints = new ArrayList<>();

    public GltfSkin(GltfNode skeletonNode, GltfFile file) {
        skeleton = file.nodes.indexOf(skeletonNode);
        file.skins.add(this);
    }

    public void addJoint(GltfNode joint, GltfFile file) {
        joints.add(file.nodes.indexOf(joint));
    }

    public void setInvBindMarticesAccessor(GltfAccessor accessor, GltfFile file) {
        inverseBindMatrices = file.accessors.indexOf(accessor);
    }
}

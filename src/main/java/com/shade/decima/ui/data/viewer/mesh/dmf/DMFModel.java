package com.shade.decima.ui.data.viewer.mesh.dmf;


import java.util.ArrayList;
import java.util.List;

public class DMFModel extends DMFNode {
    public DMFMesh mesh;
    public List<Short> boneRemapTable = new ArrayList<>();
    public Integer skeletonId;

    public DMFModel() {
        super();
        this.type = "Model";
    }

    public void setSkeleton(DMFSkeleton skeleton, DMFSceneFile scene) {
        if (!scene.skeletons.contains(skeleton))
            scene.skeletons.add(skeleton);
        skeletonId = scene.skeletons.indexOf(skeleton);
    }
}

package com.shade.decima.ui.data.viewer.model.dmf;


import java.util.HashMap;
import java.util.Map;

public class DMFModel extends DMFNode {
    public DMFMesh mesh;
//    public List<Short> boneRemapTable = new ArrayList<>();
    public Map<Short,Short> boneRemapTable = new HashMap<>();
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

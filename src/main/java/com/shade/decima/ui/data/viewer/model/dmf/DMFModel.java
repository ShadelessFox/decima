package com.shade.decima.ui.data.viewer.model.dmf;


import com.shade.util.NotNull;

public class DMFModel extends DMFNode {
    public DMFMesh mesh;
    public Integer skeletonId;

    public DMFModel() {
        super();
        this.type = "Model";
    }

    public DMFModel(@NotNull String name) {
        super();
        this.name = name;
        this.type = "Model";
    }

    public void setSkeleton(DMFSkeleton skeleton, DMFSceneFile scene) {
        if (!scene.skeletons.contains(skeleton))
            scene.skeletons.add(skeleton);
        skeletonId = scene.skeletons.indexOf(skeleton);
    }
}

package com.shade.decima.ui.data.viewer.model.model.dmf;


import com.shade.util.NotNull;

public class DMFModel extends DMFNode {
    public DMFMesh mesh;
    public Integer skeletonId;

    public DMFModel() {
        this.type = "Model";
    }

    public DMFModel(@NotNull String name) {
        this.name = name;
        this.type = "Model";
    }

    public void setSkeleton(@NotNull DMFSkeleton skeleton, @NotNull DMFSceneFile scene) {
        if (!scene.skeletons.contains(skeleton))
            scene.skeletons.add(skeleton);
        skeletonId = scene.skeletons.indexOf(skeleton);
    }
}

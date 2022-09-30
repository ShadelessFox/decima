package com.shade.decima.ui.data.viewer.mesh.dmf;


public class DMFModel extends DMFNode {
    public DMFMesh mesh;
    public Integer skeletonId;

    public DMFModel() {
        super();
        this.type = "Model";
    }

    public void setSkeleton(DMFSkeleton skeleton, DMFSceneFile scene) {
        skeletonId = scene.skeletons.indexOf(skeleton);
    }
}

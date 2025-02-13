package com.shade.decima.ui.data.viewer.model.dmf.nodes;


import com.shade.decima.ui.data.viewer.model.dmf.data.DMFMesh;
import com.shade.decima.ui.data.viewer.model.dmf.data.DMFSceneFile;
import com.shade.decima.ui.data.viewer.model.dmf.data.DMFSkeleton;
import com.shade.util.NotNull;

public class DMFModel extends DMFNode {
    public final DMFMesh mesh;
    public Integer skeletonId;

    public DMFModel(@NotNull String name, @NotNull DMFMesh mesh) {
        super(name, DMFNodeType.MODEL);
        this.mesh = mesh;
    }

    public void setSkeleton(@NotNull DMFSkeleton skeleton, @NotNull DMFSceneFile scene) {
        if (!scene.skeletons.contains(skeleton))
            scene.skeletons.add(skeleton);
        skeletonId = scene.skeletons.indexOf(skeleton);
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && (mesh == null || mesh.primitives.isEmpty());
    }
}

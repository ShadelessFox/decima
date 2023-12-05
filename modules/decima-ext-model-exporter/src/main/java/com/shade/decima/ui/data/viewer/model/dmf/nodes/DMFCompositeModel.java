package com.shade.decima.ui.data.viewer.model.dmf.nodes;

import com.shade.util.NotNull;

public class DMFCompositeModel extends DMFNode {
    public final int skeletonId;

    public DMFCompositeModel(@NotNull String name, int skeletonId) {
        super(name, DMFNodeType.SKINNED_MODEL);
        this.skeletonId = skeletonId;
    }
}

package com.shade.decima.ui.data.viewer.model.dmf;

import com.shade.util.NotNull;

public class DMFCompositeModel extends DMFNode {
    public int skeletonId;

    public DMFCompositeModel(@NotNull String name, int skeletonId) {
        super(name, DMFNodeType.SKINNED_MODEL);
        this.skeletonId = skeletonId;
    }
}

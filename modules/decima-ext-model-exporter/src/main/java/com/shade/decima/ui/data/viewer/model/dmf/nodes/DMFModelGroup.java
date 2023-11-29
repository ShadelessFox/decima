package com.shade.decima.ui.data.viewer.model.dmf.nodes;

import com.shade.util.NotNull;

public class DMFModelGroup extends DMFNode {
    public DMFModelGroup(@NotNull String name) {
        super(name, DMFNodeType.MODEL_GROUP);
    }
}

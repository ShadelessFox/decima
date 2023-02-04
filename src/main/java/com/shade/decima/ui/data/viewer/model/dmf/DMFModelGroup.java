package com.shade.decima.ui.data.viewer.model.dmf;

import com.shade.util.NotNull;

public class DMFModelGroup extends DMFNode {
    public DMFModelGroup() {
        this.type = DMFNodeType.MODEL_GROUP;
    }

    public DMFModelGroup(@NotNull String name) {
        super(name);
        this.type = DMFNodeType.MODEL_GROUP;
    }
}

package com.shade.decima.ui.data.viewer.model.model.dmf;

import com.shade.util.NotNull;

public class DMFModelGroup extends DMFNode {
    public DMFModelGroup() {
        this.type = "ModelGroup";
    }

    public DMFModelGroup(@NotNull String name) {
        super(name);
        this.type = "ModelGroup";
    }
}

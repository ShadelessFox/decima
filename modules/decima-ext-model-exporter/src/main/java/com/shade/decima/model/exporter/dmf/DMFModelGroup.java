package com.shade.decima.model.exporter.dmf;

import com.shade.util.NotNull;

public class DMFModelGroup extends DMFNode {
    public DMFModelGroup(@NotNull String name) {
        super(name, DMFNodeType.MODEL_GROUP);
    }
}

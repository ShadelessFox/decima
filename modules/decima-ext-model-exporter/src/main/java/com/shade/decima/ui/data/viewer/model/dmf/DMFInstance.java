package com.shade.decima.ui.data.viewer.model.dmf;


import com.shade.util.NotNull;

public class DMFInstance extends DMFNode {
    public final int instanceId;

    public DMFInstance(@NotNull String name, int instanceId) {
        super(name, DMFNodeType.INSTANCE);
        this.instanceId = instanceId;
    }
}

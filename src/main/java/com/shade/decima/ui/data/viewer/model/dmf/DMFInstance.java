package com.shade.decima.ui.data.viewer.model.dmf;


import com.shade.util.NotNull;

public class DMFInstance extends DMFNode {
    public Integer instanceId;

    public DMFInstance() {
        this.type = "Instance";
    }

    public DMFInstance(@NotNull String name) {
        this.name = name;
        this.type = "Instance";
    }
}

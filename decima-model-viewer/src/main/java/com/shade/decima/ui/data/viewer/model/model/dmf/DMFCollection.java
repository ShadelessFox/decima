package com.shade.decima.ui.data.viewer.model.model.dmf;

import com.shade.util.NotNull;

public class DMFCollection {
    public String name;
    public boolean enabled = true;
    public Integer parent;

    public DMFCollection(@NotNull String name) {
        this.name = name;
    }
}

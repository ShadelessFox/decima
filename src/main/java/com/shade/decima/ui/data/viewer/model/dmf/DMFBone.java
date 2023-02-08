package com.shade.decima.ui.data.viewer.model.dmf;

import com.shade.util.NotNull;

public class DMFBone {
    public final String name;
    public DMFTransform transform;
    public final int parentId;
    public boolean localSpace = false;

    public DMFBone(@NotNull String name, @NotNull DMFTransform transform, int parentId) {
        this.name = name;
        this.transform = transform;
        this.parentId = parentId;
    }
}

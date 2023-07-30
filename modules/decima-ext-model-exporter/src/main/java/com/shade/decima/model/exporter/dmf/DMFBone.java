package com.shade.decima.model.exporter.dmf;

import com.shade.util.NotNull;

public class DMFBone {
    public final String name;
    public DMFTransform transform;
    public final int parentId;
    public boolean localSpace;

    public DMFBone(@NotNull String name, @NotNull DMFTransform transform, int parentId) {
        this.name = name;
        this.transform = transform;
        this.parentId = parentId;
    }
}

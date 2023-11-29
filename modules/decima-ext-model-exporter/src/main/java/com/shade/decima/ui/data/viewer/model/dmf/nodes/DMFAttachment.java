package com.shade.decima.ui.data.viewer.model.dmf.nodes;

import com.shade.decima.ui.data.viewer.model.dmf.DMFTransform;
import com.shade.util.NotNull;

public class DMFAttachment extends DMFNode {
    public final String boneName;

    public DMFAttachment(@NotNull String name, @NotNull String boneName, @NotNull DMFTransform transform) {
        super(name, DMFNodeType.ATTACHMENT);
        this.boneName = boneName;
        this.transform = transform;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}

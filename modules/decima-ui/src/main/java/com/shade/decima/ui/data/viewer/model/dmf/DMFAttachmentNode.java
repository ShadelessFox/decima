package com.shade.decima.ui.data.viewer.model.dmf;

import com.shade.util.NotNull;

public class DMFAttachmentNode extends DMFNode {
    public final String boneName;

    public DMFAttachmentNode(@NotNull String name, @NotNull String boneName, @NotNull DMFTransform transform) {
        super(name, DMFNodeType.ATTACHMENT);
        this.boneName = boneName;
        this.transform = transform;
    }
}

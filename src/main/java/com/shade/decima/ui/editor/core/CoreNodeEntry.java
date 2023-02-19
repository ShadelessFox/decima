package com.shade.decima.ui.editor.core;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.path.RTTIPathElement;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.util.NotNull;

public class CoreNodeEntry extends CoreNodeObject {
    private final int index;

    public CoreNodeEntry(@NotNull TreeNode parent, @NotNull RTTIObject object, int index) {
        super(parent, object.type(), object.type().getFullTypeName(), new RTTIPathElement.UUID(object));
        this.index = index;
    }

    @NotNull
    public RTTIObject getObjectUUID() {
        return ((RTTIObject) getValue()).get("ObjectUUID");
    }

    public int getIndex() {
        return index;
    }
}

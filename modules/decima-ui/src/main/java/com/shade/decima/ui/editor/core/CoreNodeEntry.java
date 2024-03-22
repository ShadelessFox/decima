package com.shade.decima.ui.editor.core;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.path.RTTIPath;
import com.shade.decima.model.rtti.path.RTTIPathElement;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.util.NotNull;

public class CoreNodeEntry extends CoreNodeObject {
    private int index;

    public CoreNodeEntry(@NotNull TreeNode parent, @NotNull CoreEditor editor, @NotNull RTTIObject object) {
        super(parent, editor, object.type(), object.type().getFullTypeName(), new RTTIPath(new RTTIPathElement.UUID(object)));
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}

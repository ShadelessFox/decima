package com.shade.decima.ui.editor.core;

import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.path.RTTIPathElement;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.controls.tree.TreeNodeLazy;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

public class CoreNodeEntryGroup extends TreeNodeLazy {
    private final RTTIType<?> type;

    public CoreNodeEntryGroup(@Nullable CoreNodeBinary parent, @NotNull RTTIType<?> type) {
        super(parent);
        this.type = type;
    }

    @NotNull
    @Override
    protected TreeNode[] loadChildren(@NotNull ProgressMonitor monitor) {
        return getParentOfType(CoreNodeBinary.class).getEntries(this, type);
    }

    @NotNull
    @Override
    public String getLabel() {
        return type.getTypeName();
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return UIManager.getIcon("Node.archiveIcon");
    }

    @NotNull
    public RTTIType<?> getType() {
        return type;
    }

    public boolean contains(@NotNull RTTIPathElement.UUID element) {
        final CoreBinary binary = getParentOfType(CoreNodeBinary.class).getBinary();

        for (RTTIObject entry : binary.entries()) {
            if (entry.type() == type && element.equals(new RTTIPathElement.UUID(entry))) {
                return true;
            }
        }

        return false;
    }
}

package com.shade.decima.ui.editor.property;

import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.controls.tree.TreeNodeLazy;
import com.shade.util.NotNull;

public class PropertyRootNode extends TreeNodeLazy {
    private final CoreBinary binary;

    public PropertyRootNode(@NotNull CoreBinary binary) {
        super(null);
        this.binary = binary;
    }

    @NotNull
    @Override
    protected TreeNode[] loadChildren(@NotNull ProgressMonitor monitor) {
        return binary.entries().stream()
            .map(entry -> new PropertyObjectNode(
                this,
                entry.getType(),
                entry,
                RTTITypeRegistry.getFullTypeName(entry.getType())
            ))
            .toArray(TreeNode[]::new);
    }

    @NotNull
    @Override
    public String getLabel() {
        return "<root>";
    }
}

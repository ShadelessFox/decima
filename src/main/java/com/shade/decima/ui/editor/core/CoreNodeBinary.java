package com.shade.decima.ui.editor.core;

import com.shade.decima.model.base.CoreBinary;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.controls.tree.TreeNodeLazy;
import com.shade.util.NotNull;

public class CoreNodeBinary extends TreeNodeLazy {
    private final CoreBinary binary;

    public CoreNodeBinary(@NotNull CoreBinary binary) {
        super(null);
        this.binary = binary;
    }

    @NotNull
    @Override
    protected TreeNode[] loadChildren(@NotNull ProgressMonitor monitor) {
        return binary.entries().stream()
            .map(entry -> new CoreNodeEntry(this, entry))
            .toArray(TreeNode[]::new);
    }

    @NotNull
    @Override
    public String getLabel() {
        return "<root>";
    }
}

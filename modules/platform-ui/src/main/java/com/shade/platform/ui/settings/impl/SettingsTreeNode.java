package com.shade.platform.ui.settings.impl;

import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.controls.tree.TreeNodeLazy;
import com.shade.platform.ui.settings.SettingsRegistry;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

public class SettingsTreeNode extends TreeNodeLazy {
    protected SettingsTreeNode(@Nullable SettingsTreeNode parent) {
        super(parent);
    }

    public SettingsTreeNode() {
        super(null);
    }

    @NotNull
    @Override
    protected TreeNode[] loadChildren(@NotNull ProgressMonitor monitor) throws Exception {
        return SettingsRegistry.getInstance().getPages(getId()).stream()
            .map(page -> new SettingsTreeNodePage(this, page))
            .toArray(TreeNode[]::new);
    }

    @Override
    protected boolean allowsChildren() {
        return SettingsRegistry.getInstance().hasPages(getId());
    }

    @Override
    public boolean loadChildrenInBackground() {
        return false;
    }

    @NotNull
    @Override
    public String getLabel() {
        return "Settings";
    }

    @NotNull
    public String getId() {
        return "";
    }
}

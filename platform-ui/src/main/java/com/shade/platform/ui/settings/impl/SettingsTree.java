package com.shade.platform.ui.settings.impl;

import com.shade.platform.ui.controls.tree.Tree;
import com.shade.util.NotNull;

public class SettingsTree extends Tree {
    public SettingsTree(@NotNull SettingsDialog dialog) {
        setCellRenderer(new SettingsTreeCellRenderer(dialog));
        setRootVisible(false);

        setModel(new SettingsTreeModel(this, new SettingsTreeNode()));
    }

    @NotNull
    @Override
    public SettingsTreeModel getModel() {
        return (SettingsTreeModel) super.getModel();
    }
}

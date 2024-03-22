package com.shade.platform.ui.settings.impl;

import com.shade.platform.ui.controls.tree.Tree;
import com.shade.util.NotNull;

import javax.swing.border.EmptyBorder;

public class SettingsTree extends Tree {
    public SettingsTree(@NotNull SettingsDialog dialog) {
        setCellRenderer(new SettingsTreeCellRenderer(dialog));
        setRootVisible(false);

        setModel(new SettingsTreeModel(this, new SettingsTreeNode()));
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setBorder(new EmptyBorder(0, 8, 0, 0));
    }

    @NotNull
    @Override
    public SettingsTreeModel getModel() {
        return (SettingsTreeModel) super.getModel();
    }
}

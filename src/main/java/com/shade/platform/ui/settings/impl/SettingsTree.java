package com.shade.platform.ui.settings.impl;

import com.shade.platform.ui.controls.tree.Tree;
import com.shade.platform.ui.controls.tree.TreeModel;
import com.shade.util.NotNull;

import javax.swing.border.EmptyBorder;

public class SettingsTree extends Tree {
    public SettingsTree(@NotNull SettingsDialog dialog) {
        super(new SettingsTreeNode(), TreeModel::new);
        setCellRenderer(new SettingsTreeCellRenderer(dialog));
        setRootVisible(false);
        setBorder(new EmptyBorder(0, 8, 0, 0));
    }
}

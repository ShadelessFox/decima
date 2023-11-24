package com.shade.decima.ui.data.viewer.model.outline;

import com.shade.decima.model.viewer.isr.Node;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.platform.ui.controls.tree.TreeModel;
import com.shade.util.NotNull;

import javax.swing.tree.TreePath;

public class OutlineTree extends Tree {
    public OutlineTree(@NotNull Node root) {
        super(new OutlineTreeNode(root), TreeModel::new);
        setCellRenderer(new OutlineTreeCellRenderer());
        setSelectionPath(new TreePath(root));
    }
}

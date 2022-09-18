package com.shade.decima.ui.navigator;

import com.shade.decima.ui.navigator.impl.NavigatorNode;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.util.NotNull;

public class NavigatorTree extends Tree {
    public NavigatorTree(@NotNull NavigatorNode root) {
        super(root, (tree, node) -> new NavigatorTreeModel((NavigatorTree) tree, (NavigatorNode) node));
        setCellRenderer(new NavigatorTreeCellRenderer(getModel()));
    }

    @NotNull
    @Override
    public NavigatorTreeModel getModel() {
        return (NavigatorTreeModel) super.getModel();
    }
}

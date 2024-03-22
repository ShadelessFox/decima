package com.shade.decima.ui.navigator;

import com.shade.decima.ui.navigator.impl.NavigatorNode;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.util.NotNull;

import javax.swing.tree.TreeSelectionModel;

public class NavigatorTree extends Tree {
    public NavigatorTree(@NotNull NavigatorNode root) {
        setCellRenderer(new NavigatorTreeCellRenderer());
        getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        setModel(new NavigatorTreeModel(this, root));
    }

    @NotNull
    @Override
    public NavigatorTreeModel getModel() {
        return (NavigatorTreeModel) super.getModel();
    }
}

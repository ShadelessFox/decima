package com.shade.decima.ui.navigator;

import com.shade.decima.ui.navigator.impl.NavigatorNode;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.tree.TreeSelectionModel;
import java.awt.*;

public class NavigatorTree extends Tree {
    public NavigatorTree(@NotNull NavigatorNode root) {
        setCellRenderer(new NavigatorTreeCellRenderer());
        getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        setModel(new NavigatorTreeModel(this, root));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (getModel().isEmpty()) {
            UIUtils.setRenderingHints(g);
            UIUtils.drawCenteredString(g, "No projects", getWidth(), getHeight());
        }
    }

    @NotNull
    @Override
    public NavigatorTreeModel getModel() {
        return (NavigatorTreeModel) super.getModel();
    }
}

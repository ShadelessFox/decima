package com.shade.decima.ui.navigator;

import com.shade.decima.model.app.Workspace;
import com.shade.decima.model.util.NotNull;

import javax.swing.*;

public class NavigatorTree extends JScrollPane {
    private final NavigatorTreeModel model;
    private final JTree tree;

    public NavigatorTree(@NotNull Workspace workspace, @NotNull NavigatorNode root) {
        this.model = new NavigatorTreeModel(workspace, this, root);
        this.tree = new JTree(model);

        setViewportView(tree);
    }

    @NotNull
    public JTree getTree() {
        return tree;
    }

    @NotNull
    public NavigatorTreeModel getModel() {
        return model;
    }
}

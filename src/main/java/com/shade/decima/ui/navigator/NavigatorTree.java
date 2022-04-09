package com.shade.decima.ui.navigator;

import com.shade.decima.model.app.Workspace;
import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

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

    @Nullable
    public NavigatorNode findNode(@NotNull ProgressMonitor monitor, @NotNull NavigatorNode root, @NotNull String[] path) throws Exception {
        NavigatorNode node = root;

        outer:
        for (String part : path) {
            final NavigatorNode[] children;

            if (node instanceof NavigatorLazyNode lazy) {
                children = lazy.getChildren(monitor, model);
            } else {
                children = node.getChildren(monitor);
            }

            for (NavigatorNode child : children) {
                if (child.getLabel().equals(part)) {
                    node = child;
                    continue outer;
                }
            }

            return null;
        }

        return node;
    }
}

package com.shade.decima.ui.navigator;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.Workspace;
import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.navigator.impl.NavigatorPackfileNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;

import javax.swing.*;
import java.util.function.Predicate;

public class NavigatorTree extends JScrollPane {
    private final NavigatorTreeModel model;
    private final JTree tree;

    public NavigatorTree(@NotNull Workspace workspace, @NotNull NavigatorNode root) {
        this.model = new NavigatorTreeModel(workspace, this, root);
        this.tree = new JTree(model);
        this.tree.setCellRenderer(new NavigatorTreeCellRenderer());

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
    public NavigatorNode findFileNode(@NotNull ProgressMonitor monitor, @NotNull Project project, @NotNull Packfile packfile, @NotNull String[] path) throws Exception {
        NavigatorNode node = model.getRoot();

        node = findChild(monitor, node, child -> child instanceof NavigatorProjectNode n && n.getProject() == project);

        if (node == null) {
            return null;
        }

        node = findChild(monitor, node, child -> child instanceof NavigatorPackfileNode n && n.getPackfile() == packfile);

        if (node == null) {
            return null;
        }

        for (String part : path) {
            node = findChild(monitor, node, child -> child.getLabel().equals(part));

            if (node == null) {
                return null;
            }
        }

        return node;
    }

    @Nullable
    private NavigatorNode findChild(@NotNull ProgressMonitor monitor, @NotNull NavigatorNode root, @NotNull Predicate<NavigatorNode> predicate) throws Exception {
        final NavigatorNode[] children;

        if (root instanceof NavigatorLazyNode lazy) {
            children = lazy.getChildren(monitor, model);
        } else {
            children = root.getChildren(monitor);
        }

        for (NavigatorNode child : children) {
            if (predicate.test(child)) {
                return child;
            }
        }

        return null;
    }
}

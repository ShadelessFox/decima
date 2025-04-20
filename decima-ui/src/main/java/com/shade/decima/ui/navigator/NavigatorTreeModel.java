package com.shade.decima.ui.navigator;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.TreeModel;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.util.NotNull;

import java.util.concurrent.CompletableFuture;

public class NavigatorTreeModel extends TreeModel {
    public NavigatorTreeModel(@NotNull NavigatorTree tree, @NotNull NavigatorNode root) {
        super(tree, root);
    }

    @NotNull
    public NavigatorProjectNode getProjectNode(@NotNull ProgressMonitor monitor, @NotNull ProjectContainer container) {
        final TreeNode node;

        try {
            node = findChild(monitor, child -> child instanceof NavigatorProjectNode n && n.getProjectContainer() == container).get();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while looking for node of project " + container.getName() + " (" + container.getId() + ")", e);
        }

        if (node != null) {
            return (NavigatorProjectNode) node;
        } else {
            throw new IllegalArgumentException("Can't find node for project " + container.getName() + " (" + container.getId() + ")");
        }
    }

    @NotNull
    public CompletableFuture<NavigatorFileNode> findFileNode(@NotNull ProgressMonitor monitor, @NotNull NavigatorPath path) {
        return findChildRecursively(
            monitor,
            getRoot(),
            child -> child instanceof NavigatorNode node && node.contains(path),
            child -> child instanceof NavigatorFileNode node && node.contains(path),
            () -> "Can't find node for file '" + path.filePath().full() + "'"
        ).thenApply(node -> (NavigatorFileNode) node);
    }
}

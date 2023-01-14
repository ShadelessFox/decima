package com.shade.decima.ui.navigator;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorNode;
import com.shade.decima.ui.navigator.impl.NavigatorPackfileNode;
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
    public CompletableFuture<NavigatorFileNode> findFileNode(@NotNull ProgressMonitor monitor, @NotNull ProjectContainer container, @NotNull Packfile packfile, @NotNull String[] path) {
        CompletableFuture<? extends TreeNode> future;

        future = findChild(
            monitor,
            getRoot(),
            (child, index) -> child instanceof NavigatorProjectNode n && n.getProjectContainer().equals(container)
        );

        future = future.thenCompose(node -> findChild(
            monitor,
            node,
            (child, index) -> child instanceof NavigatorPackfileNode n && n.getPackfile().equals(packfile)
        ));

        for (String part : path) {
            future = future.thenCompose(node -> findChild(
                monitor,
                node,
                (child, index) -> child.getLabel().equals(part)
            ));
        }

        return future.thenApply(node -> (NavigatorFileNode) node);
    }
}

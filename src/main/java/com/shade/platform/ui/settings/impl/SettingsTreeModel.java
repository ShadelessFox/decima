package com.shade.platform.ui.settings.impl;

import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.platform.ui.controls.tree.TreeModel;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.util.NotNull;

import java.util.concurrent.CompletableFuture;

public class SettingsTreeModel extends TreeModel {
    public SettingsTreeModel(@NotNull Tree tree, @NotNull TreeNode root) {
        super(tree, root);
    }

    @NotNull
    public CompletableFuture<? extends TreeNode> findPageNode(@NotNull ProgressMonitor monitor, @NotNull String[] path) {
        CompletableFuture<? extends TreeNode> future = null;

        assert path.length > 0;

        for (String part : path) {
            if (future == null) {
                future = findChild(
                    monitor,
                    child -> ((SettingsTreeNode) child).getId().equals(part)
                );
            } else {
                future = future.thenCompose(node -> findChild(
                    monitor,
                    node,
                    child -> ((SettingsTreeNode) child).getId().equals(part)
                ));
            }
        }

        return future;
    }
}

package com.shade.decima.model.viewer.outline;

import com.shade.decima.model.viewer.scene.Node;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.controls.tree.TreeNodeLazy;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.util.Objects;

public class OutlineTreeNode extends TreeNodeLazy {
    private final Node node;

    public OutlineTreeNode(@NotNull Node node) {
        this(null, node);
    }

    private OutlineTreeNode(@Nullable OutlineTreeNode parent, @NotNull Node node) {
        super(parent);
        this.node = node;
    }

    @Override
    protected boolean allowsChildren() {
        return !node.getChildren().isEmpty();
    }

    @NotNull
    @Override
    protected TreeNode[] loadChildren(@NotNull ProgressMonitor monitor) {
        return node.getChildren().stream()
            .map(node -> new OutlineTreeNode(this, node))
            .toArray(TreeNode[]::new);
    }

    @NotNull
    @Override
    public String getLabel() {
        return Objects.requireNonNullElse(node.getName(), "<unnamed>");
    }

    @NotNull
    public Node getNode() {
        return node;
    }
}

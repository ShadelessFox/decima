package com.shade.platform.ui.controls.tree;

import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

public abstract class TreeNodeLazy extends TreeNode {
    private TreeNode[] children;

    public TreeNodeLazy(@Nullable TreeNode parent) {
        super(parent);
    }

    @NotNull
    @Override
    public TreeNode[] getChildren(@NotNull ProgressMonitor monitor) throws Exception {
        if (needsInitialization()) {
            synchronized (this) {
                if (needsInitialization()) {
                    children = loadChildren(monitor);
                }
            }
        }

        if (children == null) {
            children = EMPTY_CHILDREN;
        }

        return children;
    }

    public boolean needsInitialization() {
        return children == null && allowsChildren();
    }

    public void addChild(@NotNull TreeNode node, int index) {
        if (needsInitialization()) {
            throw new IllegalStateException("Node is not initialized");
        }

        final TreeNode[] result = new TreeNode[children.length + 1];
        System.arraycopy(children, 0, result, 0, index);
        System.arraycopy(children, index, result, index + 1, children.length - index);
        result[index] = node;
        children = result;
    }

    public void removeChild(int index) {
        if (needsInitialization()) {
            throw new IllegalStateException("Node is not initialized");
        }

        final TreeNode[] result = new TreeNode[children.length - 1];
        System.arraycopy(children, 0, result, 0, index);
        System.arraycopy(children, index + 1, result, index, children.length - index - 1);
        children = result;
    }

    public void clear() {
        children = null;
    }

    protected boolean allowsChildren() {
        return true;
    }

    @NotNull
    protected abstract TreeNode[] loadChildren(@NotNull ProgressMonitor monitor) throws Exception;
}

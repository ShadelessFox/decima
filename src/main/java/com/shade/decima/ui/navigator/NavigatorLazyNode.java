package com.shade.decima.ui.navigator;

import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

public abstract class NavigatorLazyNode extends NavigatorNode {
    private NavigatorNode[] children;

    public NavigatorLazyNode(@Nullable NavigatorNode parent) {
        super(parent);
    }

    @NotNull
    @Override
    public NavigatorNode[] getChildren(@NotNull ProgressMonitor monitor) throws Exception {
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

    public void addChild(@NotNull NavigatorNode node, int index) {
        if (needsInitialization()) {
            throw new IllegalStateException("Node is not initialized");
        }

        final NavigatorNode[] result = new NavigatorNode[children.length + 1];
        System.arraycopy(children, 0, result, 0, index);
        System.arraycopy(children, index, result, index + 1, children.length - index);
        result[index] = node;
        children = result;
    }

    public void removeChild(int index) {
        if (needsInitialization()) {
            throw new IllegalStateException("Node is not initialized");
        }

        final NavigatorNode[] result = new NavigatorNode[children.length - 1];
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
    protected abstract NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) throws Exception;
}

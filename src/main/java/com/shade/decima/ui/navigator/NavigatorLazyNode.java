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
            children = loadChildren(monitor);
        }

        if (children == null) {
            children = EMPTY_CHILDREN;
        }

        return children;
    }

    public boolean needsInitialization() {
        return children == null;
    }

    @NotNull
    protected abstract NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) throws Exception;
}

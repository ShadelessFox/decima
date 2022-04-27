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

        return children;
    }

    public boolean needsInitialization() {
        return children == null;
    }

    @NotNull
    protected abstract NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) throws Exception;

    public static class LoadingNode extends NavigatorNode {
        public LoadingNode(@Nullable NavigatorNode parent) {
            super(parent);
        }

        @NotNull
        @Override
        public String getLabel() {
            return "<html><font color=gray>Loading\u2026</font></html>";
        }

        @NotNull
        @Override
        public NavigatorNode[] getChildren(@NotNull ProgressMonitor monitor) throws Exception {
            return new NavigatorNode[0];
        }
    }
}

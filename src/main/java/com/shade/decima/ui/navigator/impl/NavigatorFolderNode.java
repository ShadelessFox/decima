package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.navigator.NavigatorLazyNode;
import com.shade.decima.ui.navigator.NavigatorNode;

public class NavigatorFolderNode extends NavigatorLazyNode {
    private final NavigatorNode[] children;
    private final String label;
    private final int depth;

    public NavigatorFolderNode(@Nullable NavigatorNode parent, @NotNull NavigatorNode[] children, @NotNull String label, int depth) {
        super(parent);
        this.children = children;
        this.label = label;
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }

    @NotNull
    @Override
    public String getLabel() {
        return label;
    }

    @NotNull
    @Override
    protected NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) {
        return children;
    }
}

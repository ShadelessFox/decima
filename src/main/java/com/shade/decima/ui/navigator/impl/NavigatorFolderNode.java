package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.navigator.NavigatorLazyNode;
import com.shade.decima.ui.navigator.NavigatorNode;

import java.beans.PropertyChangeListener;
import java.util.List;

public class NavigatorFolderNode extends NavigatorLazyNode {
    private final NavigatorNode parent;
    private final List<? extends NavigatorNode> children;
    private final String label;
    private final int depth;

    public NavigatorFolderNode(@Nullable NavigatorNode parent, @NotNull List<? extends NavigatorNode> children, @NotNull String label, int depth) {
        this.parent = parent;
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
    protected List<? extends NavigatorNode> loadChildren(@NotNull PropertyChangeListener listener) {
        return children;
    }

    @Nullable
    @Override
    public NavigatorNode getParent() {
        return parent;
    }
}

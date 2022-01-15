package com.shade.decima.ui.navigator;

import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;

import java.util.List;

public class NavigatorFolderNode extends NavigatorNode {
    private final NavigatorNode parent;
    private final List<NavigatorNode> children;
    private final String label;

    public NavigatorFolderNode(@Nullable NavigatorNode parent, @NotNull List<NavigatorNode> children, @NotNull String label) {
        this.parent = parent;
        this.children = children;
        this.label = label;
    }

    @Nullable
    @Override
    public String getLabel() {
        return label;
    }

    @NotNull
    @Override
    public List<NavigatorNode> getChildren() {
        return children;
    }

    @Nullable
    @Override
    public NavigatorNode getParent() {
        return parent;
    }
}

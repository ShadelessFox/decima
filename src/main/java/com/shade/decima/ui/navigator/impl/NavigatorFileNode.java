package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.navigator.NavigatorNode;

public class NavigatorFileNode extends NavigatorNode {
    private final String name;
    private final long hash;

    public NavigatorFileNode(@Nullable NavigatorNode parent, @NotNull String name, long hash) {
        super(parent);
        this.name = name;
        this.hash = hash;
    }

    @NotNull
    @Override
    public String getLabel() {
        return name;
    }

    @NotNull
    @Override
    public NavigatorNode[] getChildren(@NotNull ProgressMonitor monitor) throws Exception {
        return new NavigatorNode[0];
    }

    public long getHash() {
        return hash;
    }
}

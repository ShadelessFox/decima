package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.navigator.NavigatorNode;

import java.util.StringJoiner;

public class NavigatorFileNode extends NavigatorNode {
    private final String[] path;
    private final long hash;
    private NavigatorNode parent;
    private int depth;

    public NavigatorFileNode(@Nullable NavigatorNode parent, @NotNull String[] path, long hash) {
        super(parent);
        this.path = path;
        this.hash = hash;
        this.depth = 0;
    }

    public void setParent(@Nullable NavigatorNode parent) {
        this.parent = parent;
    }

    @Nullable
    @Override
    public NavigatorNode getParent() {
        return parent != null ? parent : super.getParent();
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    @NotNull
    @Override
    public String getLabel() {
        final StringJoiner joiner = new StringJoiner("/");
        for (int i = depth; i < path.length; i++) {
            joiner.add(path[i]);
        }
        return joiner.toString();
    }

    @NotNull
    @Override
    public NavigatorNode[] getChildren(@NotNull ProgressMonitor monitor) throws Exception {
        return new NavigatorNode[0];
    }

    @NotNull
    public String[] getPath() {
        return path;
    }

    public long getHash() {
        return hash;
    }

}

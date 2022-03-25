package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.archive.Archive;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.navigator.NavigatorNode;

import java.util.StringJoiner;

public class NavigatorFileNode extends NavigatorNode {
    private final Archive.FileEntry file;
    private final String[] path;
    private int depth;

    public NavigatorFileNode(@Nullable NavigatorNode parent, @NotNull String label, @NotNull Archive.FileEntry file) {
        super(parent);
        this.file = file;
        this.path = label.split("/");
        this.depth = 0;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    @NotNull
    public Archive.FileEntry getFile() {
        return file;
    }

    @NotNull
    public String[] getPath() {
        return path;
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
}

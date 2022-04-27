package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.UIUtils;
import com.shade.decima.ui.navigator.NavigatorLazyNode;
import com.shade.decima.ui.navigator.NavigatorNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedSet;

public class NavigatorFolderNode extends NavigatorLazyNode {
    private final FilePath path;

    public NavigatorFolderNode(@Nullable NavigatorNode parent, @NotNull FilePath path) {
        super(parent);
        this.path = path;
    }

    @NotNull
    @Override
    public String getLabel() {
        return path.last();
    }

    @NotNull
    @Override
    protected NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) throws Exception {
        final SortedSet<FilePath> files = getFilesForPath();
        final Map<FilePath, NavigatorNode> children = new LinkedHashMap<>();

        for (FilePath file : files) {
            if (file.length() - path.length() > 1) {
                children.computeIfAbsent(
                    file.slice(path.length() + 1),
                    path -> new NavigatorFolderNode(this, path)
                );
            } else {
                children.computeIfAbsent(
                    file,
                    path -> new NavigatorFileNode(this, path.last(), path.hash())
                );
            }
        }

        return children.values().toArray(NavigatorNode[]::new);
    }

    @NotNull
    protected SortedSet<FilePath> getFilesForPath() {
        return UIUtils.getParentNode(this, NavigatorPackfileNode.class).getFiles(path);
    }
}

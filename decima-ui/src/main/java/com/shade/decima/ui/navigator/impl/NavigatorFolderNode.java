package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.util.FilePath;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

public class NavigatorFolderNode extends NavigatorNode {
    private static final Comparator<NavigatorNode> CHILDREN_COMPARATOR = Comparator
        .comparingInt((NavigatorNode node) -> node instanceof NavigatorFolderNode ? -1 : 1)
        .thenComparing(NavigatorNode::getLabel);

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
        final Map<FilePath, NavigatorNode> children = new HashMap<>();

        for (FilePath file : files) {
            if (file.length() - path.length() > 1) {
                children.computeIfAbsent(
                    file.slice(path.length() + 1),
                    path -> new NavigatorFolderNode(this, path)
                );
            } else {
                children.computeIfAbsent(
                    file,
                    path -> new NavigatorFileNode(this, path)
                );
            }
        }

        return children.values().stream()
            .sorted(CHILDREN_COMPARATOR)
            .toArray(NavigatorNode[]::new);
    }

    @NotNull
    public FilePath getPath() {
        return path;
    }

    @NotNull
    protected SortedSet<FilePath> getFilesForPath() {
        return getParentOfType(NavigatorPackfileNode.class).getFiles(path);
    }
}

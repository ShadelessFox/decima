package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.util.FilePath;
import com.shade.decima.ui.navigator.NavigatorPath;
import com.shade.decima.ui.navigator.NavigatorSettings;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.AlphanumericComparator;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class NavigatorFolderNode extends NavigatorNode {
    private static final Comparator<NavigatorNode> CHILDREN_COMPARATOR = Comparator
        .comparingInt((NavigatorNode node) -> node instanceof NavigatorFolderNode ? -1 : 1)
        .thenComparing(NavigatorNode::getLabel, AlphanumericComparator.getInstance());

    private final FilePath path;

    public NavigatorFolderNode(@Nullable NavigatorNode parent, @NotNull FilePath path) {
        super(parent);
        this.path = path;
    }

    @NotNull
    @Override
    public String getLabel() {
        if (getParent() instanceof NavigatorFolderNode parent) {
            return path.subpath(parent.path.length()).full();
        } else {
            return path.last();
        }
    }

    @NotNull
    @Override
    protected NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) throws Exception {
        final SortedSet<FilePath> files = getFilesForPath();
        final Map<FilePath, NavigatorNode> children = new HashMap<>();
        final NavigatorSettings.DirectoryView directoryView = getParentOfType(NavigatorProjectNode.class).getDirectoryView();

        if (directoryView == NavigatorSettings.DirectoryView.COMPACT) {
            for (FilePath directory : getCommonPrefixes(files, path.length())) {
                if (path.equals(directory)) {
                    continue;
                }

                children.computeIfAbsent(
                    directory,
                    path -> new NavigatorFolderNode(this, path)
                );
            }
        }

        for (FilePath file : files) {
            if (file.length() - path.length() <= 1) {
                children.computeIfAbsent(
                    file,
                    path -> new NavigatorFileNode(this, file)
                );
            } else if (directoryView == NavigatorSettings.DirectoryView.DEFAULT) {
                children.computeIfAbsent(
                    file.subpath(0, path.length() + 1),
                    path -> new NavigatorFolderNode(this, path)
                );
            } else if (directoryView == NavigatorSettings.DirectoryView.FLATTEN) {
                children.computeIfAbsent(
                    file.subpath(0, file.length() - 1),
                    path -> new NavigatorFolderNode(this, path)
                );
            }
        }

        return children.values().stream()
            .sorted(CHILDREN_COMPARATOR)
            .toArray(NavigatorNode[]::new);
    }

    @Override
    public boolean contains(@NotNull NavigatorPath path) {
        return path.filePath().startsWith(this.path);
    }

    @NotNull
    public FilePath getPath() {
        return path;
    }

    @NotNull
    protected SortedSet<FilePath> getFilesForPath() {
        return getParentOfType(NavigatorPackfileNode.class).getFiles(path);
    }

    @NotNull
    private static List<FilePath> getCommonPrefixes(@NotNull Collection<FilePath> paths, int offset) {
        return paths.stream()
            .collect(Collectors.groupingBy(p -> p.parts()[offset]))
            .values().stream()
            .map(p -> getCommonPrefix(p, offset))
            .toList();
    }

    @NotNull
    private static FilePath getCommonPrefix(@NotNull List<FilePath> paths, int offset) {
        final FilePath path = paths.get(0);
        int position = Math.min(offset, path.length() - 1);

        outer:
        while (position < path.length() - 1) {
            for (FilePath other : paths) {
                if (other.length() < position || !path.name(position).equals(other.name(position))) {
                    break outer;
                }
            }

            position += 1;
        }

        return path.subpath(0, position);
    }
}

package com.shade.decima.ui.navigator.impl;

import com.shade.decima.archive.Archive;
import com.shade.decima.archive.ArchiveManager;
import com.shade.decima.rtti.objects.RTTIObject;
import com.shade.decima.ui.navigator.NavigatorLazyNode;
import com.shade.decima.ui.navigator.NavigatorNode;
import com.shade.decima.ui.resources.Project;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.*;

public class NavigatorProjectNode extends NavigatorLazyNode {
    private final NavigatorNode parent;
    private final Project project;

    public NavigatorProjectNode(@Nullable NavigatorNode parent, @NotNull Project project) {
        this.parent = parent;
        this.project = project;
    }

    @Nullable
    @Override
    public String getLabel() {
        return project.getExecutablePath().getFileName().toString();
    }

    @Nullable
    @Override
    public NavigatorNode getParent() {
        return parent;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @NotNull
    @Override
    protected List<NavigatorNode> loadChildren(@NotNull PropertyChangeListener listener) throws IOException {
        project.loadArchives();

        final ArchiveManager manager = project.getArchiveManager();
        final RTTIObject prefetch = manager.readFileObjects(project.getCompressor(), "prefetch/fullgame.prefetch").get(0);
        final Set<Archive> archives = new HashSet<>();

        for (RTTIObject file : (RTTIObject[]) prefetch.getMemberValue("Files")) {
            final String path = file.getMemberValue("Path");
            final Archive.FileEntry entry = manager.getFileEntry(path);

            if (entry != null) {
                archives.add(entry.archive());
            }
        }

        final List<NavigatorNode> children = new ArrayList<>();

        for (Archive archive : archives) {
            children.add(new NavigatorArchiveNode(this, archive));
        }

        children.sort(Comparator.comparing(NavigatorNode::getLabel));

        return children;
    }
}

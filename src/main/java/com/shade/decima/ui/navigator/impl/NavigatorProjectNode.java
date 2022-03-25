package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.archive.Archive;
import com.shade.decima.model.archive.ArchiveManager;
import com.shade.decima.model.rtti.objects.RTTICollection;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.navigator.NavigatorLazyNode;
import com.shade.decima.ui.navigator.NavigatorNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NavigatorProjectNode extends NavigatorLazyNode {
    private final Project project;

    public NavigatorProjectNode(@Nullable NavigatorNode parent, @NotNull Project project) {
        super(parent);
        this.project = project;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @NotNull
    @Override
    public String getLabel() {
        return project.getExecutablePath().getFileName().toString();
    }

    @NotNull
    @Override
    protected NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) throws IOException {
        project.loadArchives();

        final ArchiveManager manager = project.getArchiveManager();
        final RTTIObject prefetch = manager.readFileObjects(project.getCompressor(), "prefetch/fullgame.prefetch").get(0);
        final List<NavigatorNode> children = new ArrayList<>();

        for (RTTIObject file : prefetch.<RTTICollection<RTTIObject>>get("Files")) {
            final String path = file.get("Path");
            final Archive.FileEntry entry = manager.getFileEntry(path);

            if (entry != null) {
                children.add(new NavigatorFileNode(this, path, entry));
            }
        }

        children.sort(Comparator.comparing(NavigatorNode::getLabel));

        return children.toArray(NavigatorNode[]::new);
    }
}

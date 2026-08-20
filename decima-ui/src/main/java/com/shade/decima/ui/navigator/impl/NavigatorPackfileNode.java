package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.archive.Archive;
import com.shade.decima.model.archive.ArchiveFile;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.util.FilePath;
import com.shade.decima.ui.navigator.NavigatorPath;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

public class NavigatorPackfileNode extends NavigatorFolderNode {
    private final Project project;
    private final Archive archive;
    private final TreeSet<FilePath> files;

    public NavigatorPackfileNode(@NotNull NavigatorNode parent, @NotNull Archive archive) {
        super(parent, FilePath.EMPTY_PATH);
        this.project = parent.getProject();
        this.archive = archive;
        this.files = new TreeSet<>();
    }

    @NotNull
    @Override
    protected NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) throws Exception {
        final Set<Long> containing = new HashSet<>();

        try (Stream<String> allFiles = project.listAllFiles()) {
            allFiles.forEach(path -> {
                final ArchiveFile file = archive.findFile(path);
                if (file != null) {
                    files.add(new FilePath(path.split("/"), file.getIdentifier()));
                    containing.add(file.getIdentifier());
                }
            });
        }

        for (ArchiveFile file : archive.getFiles()) {
            if (!containing.contains(file.getIdentifier())) {
                files.add(new FilePath(new String[]{"%#018x".formatted(file.getIdentifier())}, file.getIdentifier()));
            }
        }

        return super.loadChildren(monitor);
    }

    @NotNull
    @Override
    public String getLabel() {
        if (archive instanceof Packfile packfile && packfile.getLanguage() != null) {
            return archive.getName() + " (" + packfile.getLanguage() + ")";
        } else {
            return archive.getName();
        }
    }

    @Nullable
    @Override
    public String getDescription() {
        return archive.getPath().toString();
    }

    @NotNull
    @Override
    public Archive getArchive() {
        return archive;
    }

    @Override
    public boolean contains(@NotNull NavigatorPath path) {
        return archive.getId().equals(path.packfileId());
    }

    @Override
    protected boolean hasChanges(@NotNull FilePath path) {
        return archive instanceof Packfile packfile && packfile.hasChangesInPath(path);
    }

    @Override
    @NotNull
    public SortedSet<FilePath> getFiles(@NotNull FilePath path) {
        return files.subSet(path, path.concat("*"));
    }

    @NotNull
    @Override
    protected ArchiveFile getArchiveFile(@NotNull FilePath path) {
        return archive.getFile(path.hash());
    }
}

package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileBase;
import com.shade.decima.model.packfile.PackfileInfo;
import com.shade.decima.model.util.FilePath;
import com.shade.decima.ui.navigator.NavigatorPath;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

public class NavigatorPackfileNode extends NavigatorFolderNode {
    private final Project project;
    private final Packfile packfile;
    private final TreeSet<FilePath> files;

    public NavigatorPackfileNode(@NotNull NavigatorProjectNode parent, @NotNull Packfile packfile) {
        super(parent, FilePath.EMPTY_PATH);
        this.project = parent.getProject();
        this.packfile = packfile;
        this.files = new TreeSet<>();
    }

    @NotNull
    @Override
    protected NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) throws Exception {
        final Set<Long> containing = new HashSet<>();

        try (Stream<String> allFiles = project.listAllFiles()) {
            allFiles.forEach(path -> {
                final long hash = PackfileBase.getPathHash(path);
                if (packfile.contains(hash)) {
                    files.add(new FilePath(path.split("/"), hash));
                    containing.add(hash);
                }
            });
        }

        for (PackfileBase.FileEntry entry : packfile.getFileEntries()) {
            if (!containing.contains(entry.hash())) {
                files.add(new FilePath(new String[]{"<unnamed>", Long.toHexString(entry.hash())}, entry.hash()));
            }
        }

        return super.loadChildren(monitor);
    }

    @NotNull
    @Override
    public String getLabel() {
        final PackfileInfo info = packfile.getInfo();
        if (info != null && info.lang() != null) {
            return packfile.getName() + " (" + info.lang().getLabel() + ")";
        } else {
            return packfile.getName();
        }
    }

    @Nullable
    @Override
    public String getDescription() {
        return packfile.getPath().toString();
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return UIManager.getIcon("Node.archiveIcon");
    }

    @NotNull
    @Override
    public Packfile getPackfile() {
        return packfile;
    }

    @Override
    public boolean contains(@NotNull NavigatorPath path) {
        return packfile.getPath().getFileName().toString().equals(path.packfileId());
    }

    @NotNull
    public SortedSet<FilePath> getFiles(@NotNull FilePath path) {
        return files.subSet(path, path.concat("*"));
    }

    @NotNull
    @Override
    protected SortedSet<FilePath> getFilesForPath() {
        return files;
    }
}

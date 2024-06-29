package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.ProjectManager;
import com.shade.decima.model.archive.ArchiveFile;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.util.FilePath;
import com.shade.decima.ui.navigator.NavigatorPath;
import com.shade.decima.ui.navigator.NavigatorSettings;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class NavigatorProjectNode extends NavigatorFolderNode {
    private final ProjectContainer container;
    private final NavigatorSettings settings;
    private Project project;

    private final TreeSet<FilePath> files = new TreeSet<>();

    public NavigatorProjectNode(@Nullable NavigatorNode parent, @NotNull ProjectContainer container) {
        super(parent, FilePath.EMPTY_PATH);
        this.container = container;
        this.settings = NavigatorSettings.getInstance().getState();
    }

    public void open() throws IOException {
        if (project == null) {
            project = ProjectManager.getInstance().openProject(container);
        }
    }

    public boolean isOpen() {
        return project != null;
    }

    @Override
    @NotNull
    public Project getProject() {
        return Objects.requireNonNull(project, "Node is not initialized");
    }

    @NotNull
    @Override
    public ProjectContainer getProjectContainer() {
        return container;
    }

    @NotNull
    @Override
    public String getLabel() {
        return container.getName();
    }

    @Nullable
    @Override
    public String getDescription() {
        return container.getExecutablePath().toString();
    }

    @NotNull
    @Override
    protected NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) throws Exception {
        open();

        return switch (getArchiveView()) {
            case GROUPED -> loadGroupedChildren(monitor);
            case MERGED -> loadMergedChildren(monitor);
            case DEFAULT -> loadDefaultChildren(monitor);
        };
    }

    @NotNull
    private NavigatorNode[] loadDefaultChildren(@NotNull ProgressMonitor monitor) {
        return project.getPackfileManager().getArchives().stream()
            .map(packfile -> new NavigatorPackfileNode(this, packfile))
            .toArray(NavigatorNode[]::new);
    }

    @NotNull
    private NavigatorNode[] loadGroupedChildren(@NotNull ProgressMonitor monitor) {
        final Map<String, List<Packfile>> groups = new LinkedHashMap<>();
        for (Packfile packfile : project.getPackfileManager().getArchives()) {
            groups.computeIfAbsent(packfile.getName(), k -> new ArrayList<>()).add(packfile);
        }

        final List<NavigatorNode> children = new ArrayList<>();
        for (Map.Entry<String, List<Packfile>> entry : groups.entrySet()) {
            final String name = entry.getKey();
            final List<Packfile> packfiles = entry.getValue();

            if (!name.isEmpty() && packfiles.size() > 1) {
                children.add(new NavigatorPackfilesNode(this, name, packfiles.toArray(Packfile[]::new)));
            } else {
                for (Packfile packfile : packfiles) {
                    children.add(new NavigatorPackfileNode(this, packfile));
                }
            }
        }

        return children.toArray(NavigatorNode[]::new);
    }

    @NotNull
    private NavigatorNode[] loadMergedChildren(@NotNull ProgressMonitor monitor) throws Exception {
        Set<Long> containing = new HashSet<>();

        try (Stream<String> allFiles = project.listAllFiles()) {
            allFiles.forEach(path -> {
                final long hash = Packfile.getPathHash(path);
                files.add(new FilePath(path.split("/"), hash));
                containing.add(hash);
            });
        }

        for (Packfile archive : project.getPackfileManager().getArchives()) {
            for (Packfile.FileEntry entry : archive.getFileEntries()) {
                if (!containing.contains(entry.hash())) {
                    files.add(new FilePath(new String[]{"%#018x".formatted(entry.hash())}, entry.hash()));
                }
            }
        }

        return super.loadChildren(monitor);
    }

    @NotNull
    @Override
    protected ArchiveFile getArchiveFile(@NotNull FilePath path) {
        return project.getPackfileManager().getFile(path.hash());
    }

    @NotNull
    @Override
    public SortedSet<FilePath> getFiles(@NotNull FilePath path) {
        return files.subSet(path, path.concat("*"));
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return UIManager.getIcon(project != null ? "Node.monitorActiveIcon" : "Node.monitorInactiveIcon");
    }

    @NotNull
    public NavigatorSettings.ArchiveView getArchiveView() {
        return settings.archiveView;
    }

    @NotNull
    public NavigatorSettings.DirectoryView getDirectoryView() {
        return settings.directoryView;
    }

    @Override
    public void unloadChildren() {
        super.unloadChildren();
        project = null;
    }

    @Override
    public boolean contains(@NotNull NavigatorPath path) {
        return container.getId().toString().equals(path.projectId());
    }

    @Override
    protected boolean hasChanges(@NotNull FilePath path) {
        return project.getPackfileManager().hasChangesInPath(path);
    }
}

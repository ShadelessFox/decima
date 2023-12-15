package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.ProjectManager;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileChangeListener;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.navigator.NavigatorPath;
import com.shade.decima.ui.navigator.NavigatorSettings;
import com.shade.decima.ui.navigator.NavigatorTreeModel;
import com.shade.platform.model.Lazy;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NavigatorProjectNode extends NavigatorNode {
    private final ProjectContainer container;
    private final Lazy<Icon> icon;
    private final NavigatorSettings settings;
    private Project project;

    public NavigatorProjectNode(@Nullable NavigatorNode parent, @NotNull ProjectContainer container) {
        super(parent);
        this.container = container;
        this.icon = Lazy.of(() -> FileSystemView.getFileSystemView().getSystemIcon(container.getExecutablePath().toFile()));
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
    protected NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) throws IOException {
        open();

        final PackfileManager manager = project.getPackfileManager();

        final PackfileChangeListener listener = (packfile, path, change) -> {
            final NavigatorTreeModel model = Application.getNavigator().getModel();

            model
                .findFileNode(new VoidProgressMonitor(), NavigatorPath.of(container, packfile, path))
                .whenComplete((node, exception) -> model.fireNodesChanged(node));
        };

        final Stream<Packfile> stream = manager.getPackfiles().stream()
            .filter(packfile -> !packfile.isEmpty())
            .peek(packfile -> packfile.addChangeListener(listener));

        if (getPackfileView() == NavigatorSettings.PackfileView.GROUPED) {
            final Map<String, List<Packfile>> groups = stream.collect(
                Collectors.groupingBy(
                    Packfile::getName,
                    LinkedHashMap::new,
                    Collectors.toList()
                ));

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
        } else {
            return stream
                .map(packfile -> new NavigatorPackfileNode(this, packfile))
                .toArray(NavigatorNode[]::new);
        }
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return icon.get();
    }

    public void resetIcon() {
        icon.clear();
    }

    @NotNull
    public NavigatorSettings.PackfileView getPackfileView() {
        return settings.packfileView;
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
}

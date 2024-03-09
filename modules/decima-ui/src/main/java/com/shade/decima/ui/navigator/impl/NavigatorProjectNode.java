package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.ProjectManager;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.ui.navigator.NavigatorPath;
import com.shade.decima.ui.navigator.NavigatorSettings;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.*;

public class NavigatorProjectNode extends NavigatorNode {
    private final ProjectContainer container;
    private final NavigatorSettings settings;
    private Project project;

    public NavigatorProjectNode(@Nullable NavigatorNode parent, @NotNull ProjectContainer container) {
        super(parent);
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
    protected NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) throws IOException {
        open();

        if (getPackfileView() == NavigatorSettings.PackfileView.GROUPED) {
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
        } else {
            return project.getPackfileManager().getArchives().stream()
                .map(packfile -> new NavigatorPackfileNode(this, packfile))
                .toArray(NavigatorNode[]::new);
        }
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return UIManager.getIcon(project != null ? "Action.monitorActiveIcon" : "Action.monitorInactiveIcon");
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

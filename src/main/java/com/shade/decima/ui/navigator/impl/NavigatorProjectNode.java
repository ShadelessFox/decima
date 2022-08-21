package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class NavigatorProjectNode extends NavigatorNode {
    private final ProjectContainer container;
    private Project project;

    public NavigatorProjectNode(@Nullable NavigatorNode parent, @NotNull ProjectContainer container) {
        super(parent);
        this.container = container;
    }

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

    @NotNull
    @Override
    protected NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) throws IOException {
        project = new Project(container);
        project.mountDefaults();

        final PackfileManager manager = project.getPackfileManager();
        final List<NavigatorPackfileNode> children = new ArrayList<>();

        for (Packfile packfile : manager.getPackfiles()) {
            if (packfile.isEmpty()) {
                continue;
            }

            children.add(new NavigatorPackfileNode(this, packfile));
        }

        children.sort(Comparator.comparing(NavigatorNode::getLabel));

        return children.toArray(NavigatorNode[]::new);
    }

    @Override
    public void clear() {
        super.clear();

        try {
            project.close();
            project = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

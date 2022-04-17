package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileManager;
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
}

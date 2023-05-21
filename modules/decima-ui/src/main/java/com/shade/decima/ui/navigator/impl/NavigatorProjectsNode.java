package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.ProjectManager;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;

import java.util.Arrays;

public class NavigatorProjectsNode extends NavigatorNode {
    public NavigatorProjectsNode() {
        super(null);
    }

    @NotNull
    @Override
    protected NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) {
        return Arrays.stream(ProjectManager.getInstance().getProjects())
            .map(project -> new NavigatorProjectNode(this, project))
            .toArray(NavigatorProjectNode[]::new);
    }

    @NotNull
    @Override
    public String getLabel() {
        return "Projects";
    }
}

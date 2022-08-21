package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.Workspace;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;

public class NavigatorWorkspaceNode extends NavigatorNode {
    private final Workspace workspace;

    public NavigatorWorkspaceNode(@NotNull Workspace workspace) {
        super(null);
        this.workspace = workspace;
    }

    @NotNull
    @Override
    protected NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) {
        return workspace.getProjects().stream()
            .map(project -> new NavigatorProjectNode(this, project))
            .toArray(NavigatorProjectNode[]::new);
    }

    @NotNull
    @Override
    public String getLabel() {
        return "Workspace";
    }
}

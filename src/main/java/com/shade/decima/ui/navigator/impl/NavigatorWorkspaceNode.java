package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.Workspace;
import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.navigator.NavigatorLazyNode;
import com.shade.decima.ui.navigator.NavigatorNode;

public class NavigatorWorkspaceNode extends NavigatorLazyNode {
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

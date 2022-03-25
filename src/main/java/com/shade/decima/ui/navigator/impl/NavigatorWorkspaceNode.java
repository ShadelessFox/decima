package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.Workspace;
import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.navigator.NavigatorNode;

public class NavigatorWorkspaceNode extends NavigatorNode {
    private final NavigatorProjectNode[] children;

    public NavigatorWorkspaceNode(@NotNull Workspace workspace) {
        super(null);
        this.children = workspace.getProjects().stream()
            .map(project -> new NavigatorProjectNode(this, project))
            .toArray(NavigatorProjectNode[]::new);
    }

    @NotNull
    @Override
    public String getLabel() {
        return "Workspace";
    }

    @NotNull
    @Override
    public NavigatorNode[] getChildren(@NotNull ProgressMonitor monitor) throws Exception {
        return children;
    }
}

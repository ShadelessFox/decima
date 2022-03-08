package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.Workspace;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.navigator.NavigatorNode;

import java.util.ArrayList;
import java.util.List;

public class NavigatorWorkspaceNode extends NavigatorNode {
    private final Workspace workspace;
    private final List<NavigatorProjectNode> children;

    public NavigatorWorkspaceNode(@NotNull Workspace workspace) {
        this.workspace = workspace;
        this.children = new ArrayList<>();

        for (Project project : workspace.getProjects()) {
            this.children.add(new NavigatorProjectNode(this, project));
        }
    }

    @NotNull
    public Workspace getWorkspace() {
        return workspace;
    }

    @NotNull
    @Override
    public String getLabel() {
        return "Workspace";
    }

    @NotNull
    @Override
    public List<NavigatorProjectNode> getChildren() {
        return children;
    }

    @Nullable
    @Override
    public NavigatorNode getParent() {
        return null;
    }
}

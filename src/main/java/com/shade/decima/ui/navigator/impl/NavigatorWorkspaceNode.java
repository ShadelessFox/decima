package com.shade.decima.ui.navigator.impl;

import com.shade.decima.Project;
import com.shade.decima.ui.navigator.NavigatorNode;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NavigatorWorkspaceNode extends NavigatorNode {
    private final List<NavigatorProjectNode> projects;

    public NavigatorWorkspaceNode() {
        this.projects = new ArrayList<>();
    }

    public void add(@NotNull Project project) {
        projects.add(new NavigatorProjectNode(this, project));
    }

    @Nullable
    @Override
    public String getLabel() {
        return "Workspace";
    }

    @NotNull
    @Override
    public List<NavigatorProjectNode> getChildren() {
        return projects;
    }

    @Nullable
    @Override
    public NavigatorNode getParent() {
        return null;
    }
}

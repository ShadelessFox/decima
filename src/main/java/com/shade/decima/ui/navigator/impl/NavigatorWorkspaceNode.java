package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.navigator.NavigatorNode;
import com.shade.decima.ui.resources.Project;
import com.shade.decima.ui.resources.Workspace;

import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.List;

public class NavigatorWorkspaceNode extends NavigatorNode {
    private final Workspace workspace;
    private final List<NavigatorProjectNode> children;

    public NavigatorWorkspaceNode(@NotNull Workspace workspace, @NotNull DefaultTreeModel model) {
        this.workspace = workspace;
        this.children = new ArrayList<>();

        this.workspace.getPropertyChangeSupport().addPropertyChangeListener(Workspace.PROJECTS_PROPERTY, e -> {
            // TODO: Can we figure a better way of constructing nodes out of objects?
            if (e.getNewValue() != null) {
                children.add(new NavigatorProjectNode(this, (Project) e.getNewValue()));
            } else {
                children.removeIf(child -> child.getProject() == e.getOldValue());
            }

            model.nodeStructureChanged(this);
        });
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

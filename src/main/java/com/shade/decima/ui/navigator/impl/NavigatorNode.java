package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.packfile.Packfile;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.TreeNodeLazy;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

public abstract class NavigatorNode extends TreeNodeLazy {
    public NavigatorNode(@Nullable NavigatorNode parent) {
        super(parent);
    }

    @NotNull
    public Packfile getPackfile() {
        return getParentOfType(NavigatorPackfileNode.class).getPackfile();
    }

    @NotNull
    public Project getProject() {
        return getParentOfType(NavigatorProjectNode.class).getProject();
    }

    @NotNull
    public ProjectContainer getProjectContainer() {
        return getParentOfType(NavigatorProjectNode.class).getProjectContainer();
    }

    @NotNull
    public <T extends NavigatorNode> T getParentOfType(@NotNull Class<T> cls) {
        final T parent = findParentOfType(cls);

        if (parent != null) {
            return parent;
        }

        throw new IllegalArgumentException("Can't find parent node of type " + cls);
    }

    @Nullable
    public <T extends NavigatorNode> T findParentOfType(@NotNull Class<T> cls) {
        for (NavigatorNode node = this; node != null; node = node.getParent()) {
            if (cls.isInstance(node)) {
                return cls.cast(node);
            }
        }

        return null;
    }

    @Nullable
    @Override
    public NavigatorNode getParent() {
        return (NavigatorNode) super.getParent();
    }

    @NotNull
    @Override
    protected abstract NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) throws Exception;
}

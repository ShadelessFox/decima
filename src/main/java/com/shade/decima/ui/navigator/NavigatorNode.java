package com.shade.decima.ui.navigator;

import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;

public abstract class NavigatorNode {
    protected static final NavigatorNode[] EMPTY_CHILDREN = new NavigatorNode[0];

    private final NavigatorNode parent;

    public NavigatorNode(@Nullable NavigatorNode parent) {
        this.parent = parent;
    }

    @Nullable
    public NavigatorNode getParent() {
        return parent;
    }

    @Nullable
    public Icon getIcon() {
        return null;
    }

    @NotNull
    public abstract String getLabel();

    @NotNull
    public abstract NavigatorNode[] getChildren(@NotNull ProgressMonitor monitor) throws Exception;

    @Override
    public String toString() {
        return getLabel();
    }

    public interface ActionListener {
        void actionPerformed(@NotNull InputEvent event);
    }
}

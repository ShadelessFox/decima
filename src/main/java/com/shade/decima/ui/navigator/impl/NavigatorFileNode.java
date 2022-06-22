package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.icon.Icons;
import com.shade.decima.ui.navigator.NavigatorNode;

import javax.swing.*;
import java.awt.event.InputEvent;

public class NavigatorFileNode extends NavigatorNode implements NavigatorNode.ActionListener {
    private final String name;
    private final long hash;

    public NavigatorFileNode(@Nullable NavigatorNode parent, @NotNull String name, long hash) {
        super(parent);
        this.name = name;
        this.hash = hash;
    }

    @NotNull
    @Override
    public String getLabel() {
        return name;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        if (name.indexOf('.') < 0) {
            return Icons.NODE_BINARY;
        } else {
            return super.getIcon();
        }
    }

    @NotNull
    @Override
    public NavigatorNode[] getChildren(@NotNull ProgressMonitor monitor) throws Exception {
        return EMPTY_CHILDREN;
    }

    public long getHash() {
        return hash;
    }

    @Override
    public void actionPerformed(@NotNull InputEvent event) {
        Application.getFrame().getEditorsPane().showEditor(this, !event.isControlDown());
        event.consume();
    }
}

package com.shade.platform.ui.controls.tree;

import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;

public abstract class TreeNode {
    protected static final TreeNode[] EMPTY_CHILDREN = new TreeNode[0];

    private final TreeNode parent;

    public TreeNode(@Nullable TreeNode parent) {
        this.parent = parent;
    }

    @Nullable
    public TreeNode getParent() {
        return parent;
    }

    @Nullable
    public Icon getIcon() {
        return null;
    }

    @NotNull
    public abstract String getLabel();

    @NotNull
    public abstract TreeNode[] getChildren(@NotNull ProgressMonitor monitor) throws Exception;

    public interface ActionListener {
        void actionPerformed(@NotNull InputEvent event);
    }
}

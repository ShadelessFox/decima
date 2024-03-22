package com.shade.platform.ui.controls.tree;

import com.shade.platform.model.Disposable;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;

public abstract class TreeNode implements Disposable {
    protected static final TreeNode[] EMPTY_CHILDREN = new TreeNode[0];

    private TreeNode parent;

    public TreeNode(@Nullable TreeNode parent) {
        this.parent = parent;
    }

    @Nullable
    public TreeNode getParent() {
        return parent;
    }

    @NotNull
    public <T extends TreeNode> T getParentOfType(@NotNull Class<T> cls) {
        final T parent = findParentOfType(cls);

        if (parent != null) {
            return parent;
        }

        throw new IllegalArgumentException("Can't find parent node of type " + cls);
    }

    @Nullable
    public <T extends TreeNode> T findParentOfType(@NotNull Class<T> cls) {
        for (TreeNode node = this; node != null; node = node.getParent()) {
            if (cls.isInstance(node)) {
                return cls.cast(node);
            }
        }

        return null;
    }

    @Nullable
    public Icon getIcon() {
        return null;
    }

    public boolean hasIcon() {
        return true;
    }

    @NotNull
    public abstract String getLabel();

    @Nullable
    public String getDescription() {
        return null;
    }

    protected boolean allowsChildren() {
        return true;
    }

    @NotNull
    public TreeNode[] getChildren(@NotNull ProgressMonitor monitor) throws Exception {
        return EMPTY_CHILDREN;
    }

    @Override
    public void dispose() {
        parent = null;
    }

    @Override
    public String toString() {
        return getLabel();
    }

    public interface ActionListener {
        void actionPerformed(@NotNull InputEvent event);
    }
}

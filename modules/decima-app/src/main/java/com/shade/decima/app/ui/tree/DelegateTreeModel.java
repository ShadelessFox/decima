package com.shade.decima.app.ui.tree;

import com.shade.decima.app.ui.util.Listeners;
import com.shade.util.NotNull;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

public abstract class DelegateTreeModel extends AbstractTreeModel {
    private final AbstractTreeModel delegate;

    public DelegateTreeModel(@NotNull AbstractTreeModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object getRoot() {
        return delegate.getRoot();
    }

    @Override
    public Object getChild(Object parent, int index) {
        return delegate.getChild(parent, index);
    }

    @Override
    public int getChildCount(Object parent) {
        return delegate.getChildCount(parent);
    }

    @Override
    public boolean isLeaf(Object node) {
        return delegate.isLeaf(node);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        delegate.valueForPathChanged(path, newValue);
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return delegate.getIndexOfChild(parent, child);
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        delegate.addTreeModelListener(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        delegate.removeTreeModelListener(l);
    }

    @Override
    public void nodeStructureChanged(@NotNull TreePath path) {
        delegate.nodeStructureChanged(path);
    }

    @Override
    public void unload(@NotNull Object parent) {
        delegate.unload(parent);
    }

    @NotNull
    @Override
    protected Listeners<TreeModelListener> listeners() {
        return delegate.listeners();
    }
}

package com.shade.decima.app.ui.tree;

import com.shade.decima.app.ui.util.Listeners;
import com.shade.util.NotNull;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public abstract class AbstractTreeModel implements TreeModel {
    private final Listeners<TreeModelListener> listeners = new Listeners<>(TreeModelListener.class);

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        throw new UnsupportedOperationException("valueForPathChanged");
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    public void nodeStructureChanged(@NotNull TreePath path) {
        listeners.broadcast().treeStructureChanged(new TreeModelEvent(this, path, null, null));
    }

    public abstract void unload(@NotNull Object parent);

    @NotNull
    protected Listeners<TreeModelListener> listeners() {
        return listeners;
    }
}

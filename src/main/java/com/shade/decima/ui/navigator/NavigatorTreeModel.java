package com.shade.decima.ui.navigator;

import com.shade.decima.model.app.Workspace;
import com.shade.decima.model.app.runtime.VoidProgressMonitor;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorFolderNode;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

public class NavigatorTreeModel implements TreeModel {
    private final NavigatorTree tree;
    private final NavigatorNode root;
    private final List<TreeModelListener> listeners;
    private final Set<NavigatorNode> pending;

    public final boolean groupByStructure;

    public NavigatorTreeModel(@NotNull Workspace workspace, @NotNull NavigatorTree tree, @NotNull NavigatorNode root) {
        this.tree = tree;
        this.root = root;
        this.listeners = new ArrayList<>();
        this.pending = new HashSet<>();

        final Preferences node = workspace.getPreferences().node("navigator");
        this.groupByStructure = node.getBoolean("group_by_structure", true);
    }

    @Nullable
    public Object getClassifierKey(@Nullable NavigatorNode parent, @NotNull NavigatorNode node) {
        if (groupByStructure && node instanceof NavigatorFileNode file) {
            final int depth = getDepth(parent);
            final String[] path = file.getPath();
            if (path.length - 1 > depth) {
                return path[depth];
            } else {
                file.setParent(parent);
                file.setDepth(depth);
                return null;
            }
        }
        return null;
    }

    @NotNull
    public String getClassifierLabel(@Nullable NavigatorNode parent, @NotNull Object key) {
        if (key instanceof Packfile packfile) {
            return packfile.getName();
        }
        if (key instanceof String str) {
            return str;
        }
        throw new IllegalArgumentException("parent=" + parent + ", key=" + key);
    }

    @NotNull
    public NavigatorNode getClassifierNode(@Nullable NavigatorNode parent, @NotNull Object key, @NotNull NavigatorNode[] children) {
        return new NavigatorFolderNode(parent, children, getClassifierLabel(parent, key), getDepth(parent) + 1);
    }

    private static int getDepth(@Nullable NavigatorNode node) {
        if (node instanceof NavigatorFolderNode folder) {
            final int depth = folder.getDepth();
            if (depth > 0) {
                return depth;
            }
        }
        return 0;
    }

    @NotNull
    @Override
    public NavigatorNode getRoot() {
        return root;
    }

    @NotNull
    @Override
    public NavigatorNode getChild(Object parent, int index) {
        if (parent instanceof NavigatorLazyNode node && node.needsInitialization()) {
            final NavigatorLazyNode.LoadingNode placeholder = new NavigatorLazyNode.LoadingNode(node);

            if (!pending.contains(node)) {
                new LoadingWorker(node, placeholder).execute();
            }

            return placeholder;
        }

        try {
            return ((NavigatorNode) parent).getChildren(new VoidProgressMonitor())[index];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof NavigatorLazyNode node && node.needsInitialization()) {
            return 1;
        }

        try {
            return ((NavigatorNode) parent).getChildren(new VoidProgressMonitor()).length;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        for (int i = 0; i < getChildCount(parent); i++) {
            if (getChild(parent, i).equals(child)) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public boolean isLeaf(Object node) {
        return getChildCount(node) == 0;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        throw new IllegalStateException("Model does not support value changing");
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    @NotNull
    public NavigatorNode[] getPathToRoot(@NotNull NavigatorNode node) {
        return getPathToRoot(node, 0);
    }

    @NotNull
    public NavigatorNode[] getPathToRoot(@NotNull NavigatorNode node, int depth) {
        final NavigatorNode parent = node.getParent();
        final NavigatorNode[] nodes;

        if (node == root || parent == null) {
            nodes = new NavigatorNode[depth + 1];
        } else {
            nodes = getPathToRoot(parent, depth + 1);
        }

        nodes[nodes.length - depth - 1] = node;

        return nodes;
    }

    public void fireStructureChanged(@NotNull NavigatorNode node) {
        fireNodeEvent(TreeModelListener::treeStructureChanged, () -> new TreeModelEvent(this, getPathToRoot(node), null, null));
    }

    public void fireNodesChanged(@NotNull NavigatorNode node, @NotNull int[] childIndices) {
        fireNodeEvent(TreeModelListener::treeNodesChanged, () -> new TreeModelEvent(this, getPathToRoot(node), childIndices, null));
    }

    public void fireNodesInserted(@NotNull NavigatorNode node, @NotNull int[] childIndices) {
        fireNodeEvent(TreeModelListener::treeNodesInserted, () -> new TreeModelEvent(this, getPathToRoot(node), childIndices, null));
    }

    public void fireNodesRemoved(@NotNull NavigatorNode node, @NotNull int[] childIndices) {
        fireNodeEvent(TreeModelListener::treeNodesRemoved, () -> new TreeModelEvent(this, getPathToRoot(node), childIndices, null));
    }

    private void fireNodeEvent(@NotNull BiConsumer<TreeModelListener, TreeModelEvent> consumer, @NotNull Supplier<TreeModelEvent> supplier) {
        if (listeners.isEmpty()) {
            return;
        }
        final TreeModelEvent event = supplier.get();
        for (TreeModelListener listener : listeners) {
            consumer.accept(listener, event);
        }
    }

    private class LoadingWorker extends SwingWorker<NavigatorNode[], Void> {
        private final NavigatorLazyNode parent;
        private final NavigatorNode placeholder;

        public LoadingWorker(@NotNull NavigatorLazyNode parent, @NotNull NavigatorNode placeholder) {
            this.parent = parent;
            this.placeholder = placeholder;
        }

        @Override
        protected NavigatorNode[] doInBackground() throws Exception {
            try {
                pending.add(parent);
                return parent.getChildren(new VoidProgressMonitor(), NavigatorTreeModel.this);
            } finally {
                pending.remove(parent);
            }
        }

        @Override
        protected void done() {
            final JTree tree = NavigatorTreeModel.this.tree.getTree();
            final TreePath selection = tree.getSelectionPath();
            final NavigatorNode[] children;

            try {
                children = get();
            } catch (Exception e) {
                tree.collapsePath(new TreePath(getPathToRoot(placeholder)));
                throw new RuntimeException(e);
            } finally {
                fireStructureChanged(parent);
            }

            if (selection != null && selection.getLastPathComponent() == placeholder && children.length > 0) {
                tree.setSelectionPath(new TreePath(getPathToRoot(children[0])));
            }
        }
    }
}

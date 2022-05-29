package com.shade.decima.ui.navigator;

import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.app.runtime.VoidProgressMonitor;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class NavigatorTreeModel implements TreeModel {
    private final NavigatorTree tree;
    private final NavigatorNode root;
    private final List<TreeModelListener> listeners;

    /**
     * Currently loading node to placeholder node mappings
     */
    private final Map<NavigatorNode, NavigatorNode> pending;

    public NavigatorTreeModel(@NotNull NavigatorTree tree, @NotNull NavigatorNode root) {
        this.tree = tree;
        this.root = root;
        this.listeners = new ArrayList<>();
        this.pending = new HashMap<>();
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
            return pending.computeIfAbsent(node, key -> {
                final LoadingNode placeholder = new LoadingNode(key);
                final LoadingWorker worker = new LoadingWorker(key, placeholder);
                worker.execute();

                return placeholder;
            });
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
        return getPathToRoot(root, node, 0);
    }

    @NotNull
    private NavigatorNode[] getPathToRoot(@NotNull NavigatorNode root, @NotNull NavigatorNode node, int depth) {
        final NavigatorNode parent = node.getParent();
        final NavigatorNode[] nodes;

        if (node == root || parent == null) {
            nodes = new NavigatorNode[depth + 1];
        } else {
            nodes = getPathToRoot(root, parent, depth + 1);
        }

        nodes[nodes.length - depth - 1] = node;

        return nodes;
    }

    public void fireStructureChanged(@NotNull NavigatorNode node) {
        fireNodeEvent(TreeModelListener::treeStructureChanged, () -> new TreeModelEvent(this, getPathToRoot(node), null, null));
    }

    public void fireNodesChanged(@NotNull NavigatorNode node, @NotNull int... childIndices) {
        fireNodeEvent(TreeModelListener::treeNodesChanged, () -> new TreeModelEvent(this, getPathToRoot(node), childIndices, null));
    }

    public void fireNodesInserted(@NotNull NavigatorNode node, @NotNull int... childIndices) {
        fireNodeEvent(TreeModelListener::treeNodesInserted, () -> new TreeModelEvent(this, getPathToRoot(node), childIndices, null));
    }

    public void fireNodesRemoved(@NotNull NavigatorNode node, @NotNull int... childIndices) {
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
        private final NavigatorNode parent;
        private final NavigatorNode placeholder;

        public LoadingWorker(@NotNull NavigatorNode parent, @NotNull NavigatorNode placeholder) {
            this.parent = parent;
            this.placeholder = placeholder;
        }

        @Override
        protected NavigatorNode[] doInBackground() throws Exception {
            return parent.getChildren(new VoidProgressMonitor());
        }

        @Override
        protected void done() {
            pending.remove(parent);

            final JTree tree = NavigatorTreeModel.this.tree.getTree();
            final TreePath selection = tree.getSelectionPath();
            final NavigatorNode[] children;

            try {
                children = get();
            } catch (Exception e) {
                tree.collapsePath(new TreePath(getPathToRoot(parent)));
                throw new RuntimeException(e);
            } finally {
                fireStructureChanged(parent);
            }

            if (selection != null) {
                if (selection.getLastPathComponent() == placeholder && children.length > 0) {
                    // Selection was on the placeholder element, replace it with first children, if any
                    tree.setSelectionPath(new TreePath(getPathToRoot(children[0])));
                } else if (parent.getParent() == null) {
                    // The entire tree is rebuilt after changing structure of the root element, restore selection
                    tree.setSelectionPath(selection);
                    tree.scrollPathToVisible(selection);
                }
            }
        }
    }

    private static class LoadingNode extends NavigatorNode {
        public LoadingNode(@Nullable NavigatorNode parent) {
            super(parent);
        }

        @NotNull
        @Override
        public String getLabel() {
            return "<html><font color=gray>Loading\u2026</font></html>";
        }

        @NotNull
        @Override
        public NavigatorNode[] getChildren(@NotNull ProgressMonitor monitor) throws Exception {
            return EMPTY_CHILDREN;
        }
    }
}

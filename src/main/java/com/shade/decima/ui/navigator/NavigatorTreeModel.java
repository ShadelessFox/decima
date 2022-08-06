package com.shade.decima.ui.navigator;

import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.app.runtime.VoidProgressMonitor;
import com.shade.decima.model.util.IOUtils;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.icon.LoadingIcon;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class NavigatorTreeModel implements TreeModel {
    private final NavigatorTree tree;
    private final NavigatorNode root;
    private final List<TreeModelListener> listeners;

    private final Map<NavigatorNode, LoadingNode> placeholders = Collections.synchronizedMap(new HashMap<>());
    private final Map<NavigatorNode, LoadingWorker> workers = Collections.synchronizedMap(new HashMap<>());
    private final LoadingIcon loadingNodeIcon = new LoadingIcon();

    public NavigatorTreeModel(@NotNull NavigatorTree tree, @NotNull NavigatorNode root) {
        this.tree = tree;
        this.root = root;
        this.listeners = new ArrayList<>();

        final Timer timer = new Timer(1000 / LoadingIcon.SEGMENTS, e -> {
            if (placeholders.isEmpty()) {
                return;
            }

            for (NavigatorNode node : List.copyOf(placeholders.values())) {
                final TreePath path = new TreePath(getPathToRoot(node));
                final Rectangle bounds = tree.getPathBounds(path);

                if (bounds == null) {
                    continue;
                }

                tree.repaint(bounds);
            }

            loadingNodeIcon.advance();
        });

        tree.addHierarchyListener(e -> {
            if (e.getID() == HierarchyEvent.HIERARCHY_CHANGED && (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (tree.isShowing()) {
                    timer.start();
                } else {
                    timer.stop();
                }
            }
        });
    }

    @NotNull
    @Override
    public NavigatorNode getRoot() {
        return root;
    }

    @NotNull
    @Override
    public NavigatorNode getChild(Object parent, int index) {
        final CompletableFuture<NavigatorNode[]> future = getChildrenAsync(new VoidProgressMonitor(), (NavigatorNode) parent);

        if (future.isDone()) {
            return IOUtils.unchecked(future::get)[index];
        } else {
            return placeholders.computeIfAbsent((NavigatorNode) parent, LoadingNode::new);
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
        if (node instanceof NavigatorLazyNode lazy) {
            return !lazy.allowsChildren();
        } else {
            return getChildCount(node) == 0;
        }
    }

    @NotNull
    public CompletableFuture<NavigatorNode[]> getChildrenAsync(@NotNull ProgressMonitor monitor, @NotNull NavigatorNode parent) {
        if (parent instanceof NavigatorLazyNode lazy && lazy.needsInitialization()) {
            return workers.computeIfAbsent(parent, key -> {
                final CompletableFuture<NavigatorNode[]> future = new CompletableFuture<>();
                final LoadingWorker worker = new LoadingWorker(monitor, parent, future);

                worker.execute();

                return worker;
            }).future;
        } else {
            try {
                return CompletableFuture.completedFuture(parent.getChildren(monitor));
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        }
    }

    public boolean isLoading(@NotNull NavigatorNode node) {
        return node instanceof LoadingNode;
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
        return getPathToRoot(root, node);
    }

    @NotNull
    public NavigatorNode[] getPathToRoot(@NotNull NavigatorNode root, @NotNull NavigatorNode node) {
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
        private final ProgressMonitor monitor;
        private final NavigatorNode parent;
        private final CompletableFuture<NavigatorNode[]> future;

        public LoadingWorker(@NotNull ProgressMonitor monitor, @NotNull NavigatorNode parent, @NotNull CompletableFuture<NavigatorNode[]> future) {
            this.monitor = monitor;
            this.parent = parent;
            this.future = future;
        }

        @Override
        protected NavigatorNode[] doInBackground() throws Exception {
            return parent.getChildren(monitor);
        }

        @Override
        protected void done() {
            workers.remove(parent);

            final LoadingNode placeholder = placeholders.remove(parent);
            final NavigatorNode[] children;
            final TreePath selection;

            try {
                children = get();
                selection = tree.getSelectionPath();

                future.complete(children);
            } catch (Throwable e) {
                future.completeExceptionally(e);

                if (placeholder != null) {
                    tree.collapsePath(new TreePath(getPathToRoot(parent)));

                    // If there was a placeholder then this node was visible and likely was expanded by the user
                    // The getChild method does not wait for the result and will not caught this exception, so
                    // we must throw it manually, otherwise it will be lost
                    throw new RuntimeException(e);
                } else {
                    return;
                }
            } finally {
                fireStructureChanged(parent);
            }

            if (selection != null) {
                if (children.length > 0 && placeholder != null && selection.getLastPathComponent() == placeholder) {
                    // Selection was on the placeholder element, replace it with the first child
                    tree.setSelectionPath(new TreePath(getPathToRoot(children[0])));
                } else if (parent.getParent() == null) {
                    // The entire tree is rebuilt after changing structure of the root element, restore selection
                    tree.setSelectionPath(selection);
                    tree.scrollPathToVisible(selection);
                }
            }
        }
    }

    private class LoadingNode extends NavigatorNode {
        public LoadingNode(@Nullable NavigatorNode parent) {
            super(parent);
        }

        @NotNull
        @Override
        public String getLabel() {
            return "Loading\u2026";
        }

        @Nullable
        @Override
        public Icon getIcon() {
            return loadingNodeIcon;
        }

        @NotNull
        @Override
        public NavigatorNode[] getChildren(@NotNull ProgressMonitor monitor) {
            return EMPTY_CHILDREN;
        }
    }
}

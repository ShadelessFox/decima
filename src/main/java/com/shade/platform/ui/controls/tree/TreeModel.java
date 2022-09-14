package com.shade.platform.ui.controls.tree;

import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.icons.LoadingIcon;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class TreeModel implements javax.swing.tree.TreeModel {
    protected final Tree tree;

    private final TreeNode root;
    private final List<TreeModelListener> listeners;

    private final Map<TreeNode, LoadingNode> placeholders = Collections.synchronizedMap(new HashMap<>());
    private final Map<TreeNode, LoadingWorker> workers = Collections.synchronizedMap(new HashMap<>());
    private final LoadingIcon loadingNodeIcon = new LoadingIcon();

    private Predicate<? super TreeNode> filter;

    public TreeModel(@NotNull Tree tree, @NotNull TreeNode root) {
        this.tree = tree;
        this.root = root;
        this.listeners = new ArrayList<>();

        final Timer timer = new Timer(1000 / LoadingIcon.SEGMENTS, e -> {
            if (placeholders.isEmpty()) {
                return;
            }

            for (TreeNode node : List.copyOf(placeholders.values())) {
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
    public TreeNode getRoot() {
        return root;
    }

    @NotNull
    @Override
    public TreeNode getChild(Object parent, int index) {
        final CompletableFuture<TreeNode[]> future = getChildrenAsync(new VoidProgressMonitor(), (TreeNode) parent);

        if (future.isDone()) {
            return IOUtils.unchecked(future::get)[index];
        } else {
            return placeholders.computeIfAbsent((TreeNode) parent, LoadingNode::new);
        }
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof TreeNodeLazy node && node.needsInitialization()) {
            return 1;
        }

        try {
            return getChildren(new VoidProgressMonitor(), (TreeNode) parent).length;
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
        if (node instanceof TreeNodeLazy lazy) {
            return !lazy.allowsChildren();
        } else {
            return getChildCount(node) == 0;
        }
    }

    @NotNull
    public CompletableFuture<TreeNode[]> getChildrenAsync(@NotNull ProgressMonitor monitor, @NotNull TreeNode parent) {
        if (parent instanceof TreeNodeLazy lazy && lazy.needsInitialization()) {
            return workers.computeIfAbsent(parent, key -> {
                final CompletableFuture<TreeNode[]> future = new CompletableFuture<>();
                final LoadingWorker worker = new LoadingWorker(monitor, key, future);

                worker.execute();

                return worker;
            }).future;
        } else {
            try {
                return CompletableFuture.completedFuture(getChildren(monitor, parent));
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        }
    }

    public boolean isLoading(@NotNull TreeNode node) {
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
    public TreeNode[] getPathToRoot(@NotNull TreeNode node) {
        return getPathToRoot(root, node);
    }

    @NotNull
    public TreeNode[] getPathToRoot(@NotNull TreeNode root, @NotNull TreeNode node) {
        return getPathToRoot(root, node, 0);
    }

    @Nullable
    public Predicate<? super TreeNode> getFilter() {
        return filter;
    }

    public void setFilter(@Nullable Predicate<? super TreeNode> filter) {
        this.filter = filter;
        fireStructureChanged(getRoot());
    }

    @NotNull
    private TreeNode[] getPathToRoot(@NotNull TreeNode root, @NotNull TreeNode node, int depth) {
        final TreeNode parent = node.getParent();
        final TreeNode[] nodes;

        if (node == root || parent == null) {
            nodes = new TreeNode[depth + 1];
        } else {
            nodes = getPathToRoot(root, parent, depth + 1);
        }

        nodes[nodes.length - depth - 1] = node;

        return nodes;
    }

    public void fireStructureChanged(@NotNull TreeNode node) {
        fireNodeEvent(TreeModelListener::treeStructureChanged, () -> new TreeModelEvent(this, getPathToRoot(node), null, null));
    }

    public void fireNodesChanged(@NotNull TreeNode node, @NotNull int... childIndices) {
        fireNodeEvent(TreeModelListener::treeNodesChanged, () -> new TreeModelEvent(this, getPathToRoot(node), childIndices, null));
    }

    public void fireNodesInserted(@NotNull TreeNode node, @NotNull int... childIndices) {
        fireNodeEvent(TreeModelListener::treeNodesInserted, () -> new TreeModelEvent(this, getPathToRoot(node), childIndices, null));
    }

    public void fireNodesRemoved(@NotNull TreeNode node, @NotNull int... childIndices) {
        fireNodeEvent(TreeModelListener::treeNodesRemoved, () -> new TreeModelEvent(this, getPathToRoot(node), childIndices, null));
    }

    @NotNull
    public CompletableFuture<? extends TreeNode> findChild(@NotNull ProgressMonitor monitor, @NotNull TreeNode parent, @NotNull Predicate<TreeNode> predicate) {
        return getChildrenAsync(monitor, parent).thenApply(children -> {
            for (TreeNode child : children) {
                if (predicate.test(child)) {
                    return child;
                }
            }

            throw new IllegalArgumentException("Can't find node");
        });
    }

    @NotNull
    public CompletableFuture<? extends TreeNode> findChild(@NotNull ProgressMonitor monitor, @NotNull Predicate<TreeNode> predicate) {
        return findChild(monitor, getRoot(), predicate);
    }

    @NotNull
    private TreeNode[] getChildren(@NotNull ProgressMonitor monitor, @NotNull TreeNode parent) throws Exception {
        final TreeNode[] children = parent.getChildren(monitor);
        final Predicate<? super TreeNode> filter = getFilter();

        if (filter != null && children.length > 0) {
            return Arrays.stream(children)
                .filter(filter)
                .toArray(TreeNode[]::new);
        }

        return children;
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

    private class LoadingWorker extends SwingWorker<TreeNode[], Void> {
        private final ProgressMonitor monitor;
        private final TreeNode parent;
        private final CompletableFuture<TreeNode[]> future;

        public LoadingWorker(@NotNull ProgressMonitor monitor, @NotNull TreeNode parent, @NotNull CompletableFuture<TreeNode[]> future) {
            this.monitor = monitor;
            this.parent = parent;
            this.future = future;
        }

        @Override
        protected TreeNode[] doInBackground() throws Exception {
            return getChildren(monitor, parent);
        }

        @Override
        protected void done() {
            workers.remove(parent);

            final LoadingNode placeholder = placeholders.remove(parent);
            final TreeNode[] children;
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

    private class LoadingNode extends TreeNode {
        public LoadingNode(@Nullable TreeNode parent) {
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
        public TreeNode[] getChildren(@NotNull ProgressMonitor monitor) {
            return EMPTY_CHILDREN;
        }
    }
}

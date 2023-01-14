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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
                final TreePath path = getTreePathToRoot(node);
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

        if (!future.isDone()) {
            return placeholders.computeIfAbsent((TreeNode) parent, LoadingNode::new);
        }

        final TreeNode[] children = IOUtils.unchecked(future::get);

        if (children.length > 0) {
            return children[index];
        } else {
            return new EmptyNode((TreeNode) parent);
        }
    }

    @Override
    public int getChildCount(Object parent) {
        if (isSpecial((TreeNode) parent)) {
            return 0;
        }

        if (parent instanceof TreeNodeLazy node && node.needsInitialization() && node.loadChildrenInBackground()) {
            return 1;
        }

        final TreeNode[] children;

        try {
            children = getChildren(new VoidProgressMonitor(), (TreeNode) parent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return Math.max(children.length, 1);
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (isSpecial((TreeNode) child)) {
            return 0;
        }

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

    public void unloadNode(@NotNull TreeNodeLazy node) {
        if (!node.needsInitialization()) {
            node.unloadChildren();
            tree.collapsePath(getTreePathToRoot(node));
            fireStructureChanged(node);
        }
    }

    @NotNull
    public CompletableFuture<TreeNode[]> getChildrenAsync(@NotNull ProgressMonitor monitor, @NotNull TreeNode parent) {
        if (parent instanceof TreeNodeLazy lazy && lazy.needsInitialization()) {
            if (lazy.loadChildrenInBackground()) {
                return workers.computeIfAbsent(parent, key -> {
                    final LoadingWorker worker = new LoadingWorker(monitor, key, new CompletableFuture<>());
                    worker.execute();
                    return worker;
                }).future;
            }

            return getChildrenSync(monitor, parent).thenApply(nodes -> {
                fireStructureChanged(parent);
                return nodes;
            });
        }

        return getChildrenSync(monitor, parent);
    }

    @NotNull
    private CompletableFuture<TreeNode[]> getChildrenSync(@NotNull ProgressMonitor monitor, @NotNull TreeNode parent) {
        try {
            return CompletableFuture.completedFuture(getChildren(monitor, parent));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public boolean isSpecial(@NotNull TreeNode node) {
        return node instanceof LoadingNode
            || node instanceof EmptyNode;
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
    public TreePath getTreePathToRoot(@NotNull TreeNode node) {
        return new TreePath(getPathToRoot(root, node));
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

    public void fireNodesChanged(@NotNull TreeNode node) {
        fireNodeEvent(TreeModelListener::treeNodesChanged, () -> new TreeModelEvent(this, getPathToRoot(node), null, null));
    }

    public void fireNodesInserted(@NotNull TreeNode node, @NotNull int... childIndices) {
        fireNodeEvent(TreeModelListener::treeNodesInserted, () -> new TreeModelEvent(this, getPathToRoot(node), childIndices, null));
    }

    public void fireNodesRemoved(@NotNull TreeNode node, @NotNull int... childIndices) {
        fireNodeEvent(TreeModelListener::treeNodesRemoved, () -> new TreeModelEvent(this, getPathToRoot(node), childIndices, null));
    }

    @NotNull
    public CompletableFuture<? extends TreeNode> findChild(@NotNull ProgressMonitor monitor, @NotNull TreeNode parent, @NotNull NodePredicate predicate) {
        return getChildrenAsync(monitor, parent).thenApply(children -> {
            for (int i = 0; i < children.length; i++) {
                final TreeNode child = children[i];
                if (predicate.test(child, i)) {
                    return child;
                }
            }

            throw new IllegalArgumentException("Can't find node that matches the given predicate in parent node '" + parent.getLabel() + "'");
        });
    }

    @NotNull
    public CompletableFuture<? extends TreeNode> findChild(@NotNull ProgressMonitor monitor, @NotNull NodePredicate predicate) {
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

    public interface NodePredicate {
        boolean test(@NotNull TreeNode node, int index);
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
            } catch (ExecutionException e) {
                future.completeExceptionally(e.getCause());

                if (placeholder != null) {
                    tree.collapsePath(getTreePathToRoot(parent));

                    // If there was a placeholder then this node was visible and likely was expanded by the user
                    // The getChild method does not wait for the result and will not caught this exception, so
                    // we must throw it manually, otherwise it will be lost
                    throw new IllegalStateException(e.getCause());
                } else {
                    return;
                }
            } catch (CancellationException | InterruptedException e) {
                future.cancel(true);
                return;
            } finally {
                fireStructureChanged(parent);
            }

            if (selection != null) {
                if (children.length > 0 && placeholder != null && selection.getLastPathComponent() == placeholder) {
                    // Selection was on the placeholder element, replace it with the first child
                    tree.setSelectionPath(getTreePathToRoot(children[0]));
                } else if (parent.getParent() == null && selection.getLastPathComponent() != parent) {
                    // The entire tree is rebuilt after changing structure of the root element, restore selection
                    tree.setSelectionPath(selection);
                    tree.scrollPathToVisible(selection);
                }
            }

            future.complete(children);
        }
    }

    private class LoadingNode extends TreeNode {
        public LoadingNode(@NotNull TreeNode parent) {
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

    private static class EmptyNode extends TreeNode {
        public EmptyNode(@NotNull TreeNode parent) {
            super(parent);
        }

        @NotNull
        @Override
        public String getLabel() {
            return "Empty";
        }

        @Override
        public boolean hasIcon() {
            return false;
        }

        @NotNull
        @Override
        public TreeNode[] getChildren(@NotNull ProgressMonitor monitor) {
            return EMPTY_CHILDREN;
        }
    }
}

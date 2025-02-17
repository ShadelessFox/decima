package com.shade.decima.app.ui.tree;

import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 *
 * A tree model that supports paginated loading of children from an underlying delegate model.
 * <p>
 * When a node is expanded, all of its children are loaded, but just a subset of them are shown.
 * The size of the subset is determined by the {@code limit} parameter. When the user clicks on
 * the "Load more" node, the next page of children is loaded, and the tree model is updated to
 * show the new children.
 * <p>
 * This model can be combined with any other tree model, as long as the underlying model
 * inherits from {@link AbstractTreeModel}.
 *
 * @see AbstractTreeModel
 */
public final class PaginatedTreeModel extends DelegateTreeModel {
    private static final Logger log = LoggerFactory.getLogger(PaginatedTreeModel.class);
    private final int limit;

    public PaginatedTreeModel(int limit, @NotNull AbstractTreeModel model) {
        super(model);
        this.limit = limit;
    }

    @Override
    public Object getRoot() {
        return new PaginatedNode(null, super.getRoot(), limit);
    }

    @Override
    public Object getChild(Object parent, int index) {
        var node = unwrap(parent);
        if (node.loaded >= 0 && node.loaded == index) {
            return new ExpanderNode(node, super.getChildCount(node.element));
        } else {
            return new PaginatedNode(node, super.getChild(node.element, index), limit);
        }
    }

    @Override
    public int getChildCount(Object parent) {
        var node = unwrap(parent);
        var count = super.getChildCount(node.element);
        if (node.loaded >= 0 && node.loaded < count) {
            return node.loaded + 1; // +1 for expander
        } else {
            return count;
        }
    }

    @Override
    public boolean isLeaf(Object parent) {
        return switch (parent) {
            case PaginatedNode n -> super.isLeaf(n.element);
            case ExpanderNode ignored -> true;
            default -> throw new IllegalArgumentException();
        };
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        var node = unwrap(parent);
        return switch (child) {
            case PaginatedNode n -> super.getIndexOfChild(node.element, n.element);
            case ExpanderNode ignored -> node.loaded;
            default -> throw new IllegalArgumentException();
        };
    }

    @Override
    public void unload(@NotNull Object parent) {
        var node = unwrap(parent);
        node.loaded = limit;
        super.unload(node.element);
    }

    public boolean handleNextPage(@Nullable Object node, boolean loadFully) {
        if (isNextPage(node)) {
            loadNextPage(node, loadFully);
            return true;
        }
        return false;
    }

    public boolean isNextPage(@Nullable Object node) {
        return node instanceof ExpanderNode;
    }

    public void loadNextPage(@NotNull Object node, boolean loadFully) {
        if (!(node instanceof ExpanderNode(var parent, var total))) {
            throw new IllegalArgumentException("Invalid node. Use #isNextPage to check if it's a correct node");
        }

        int oldLimit = parent.loaded;
        int newLimit = loadFully ? total : Math.min(parent.loaded + limit, total);

        var listener = listeners().broadcast();
        var path = getPathToRoot(parent);

        if (newLimit == total) {
            parent.loaded = -1;
        } else {
            parent.loaded = newLimit;
        }

        log.debug("Loading next page for {} from {} to {}", parent, oldLimit, newLimit);

        if (oldLimit != newLimit) {
            listener.treeNodesInserted(new TreeModelEvent(this, path, IntStream.range(oldLimit, newLimit).toArray(), null));
        }

        if (newLimit == total) {
            listener.treeNodesRemoved(new TreeModelEvent(this, path, new int[]{newLimit}, new Object[]{node}));
        } else {
            listener.treeNodesChanged(new TreeModelEvent(this, path, new int[]{newLimit}, new Object[]{node}));
        }
    }

    @NotNull
    private static PaginatedNode unwrap(@NotNull Object node) {
        return (PaginatedNode) node;
    }

    @NotNull
    private PaginatedNode[] getPathToRoot(@NotNull PaginatedNode node) {
        return getPathToRoot(node, 0);
    }

    @NotNull
    private PaginatedNode[] getPathToRoot(@NotNull PaginatedNode node, int depth) {
        PaginatedNode parent = node.parent;
        PaginatedNode[] nodes;

        if (parent == null) {
            nodes = new PaginatedNode[depth + 1];
        } else {
            nodes = getPathToRoot(parent, depth + 1);
        }

        nodes[nodes.length - depth - 1] = node;

        return nodes;
    }

    private record ExpanderNode(@NotNull PaginatedNode parent, int total) {
        @Override
        public String toString() {
            return MessageFormat.format(UIManager.getString("PaginatedTreeModel.loadMoreText"), total);
        }
    }

    private static class PaginatedNode implements TreeItem<Object> {
        private final PaginatedNode parent;
        private final Object element;
        private int loaded; // -1 means completely loaded

        public PaginatedNode(@Nullable PaginatedNode parent, @NotNull Object element, int loaded) {
            this.parent = parent;
            this.element = element;
            this.loaded = loaded;
        }

        @Nullable
        @Override
        public Object getValue() {
            if (element instanceof TreeItem<?> item) {
                return item.getValue();
            }
            return element;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof PaginatedNode that
                && Objects.equals(parent, that.parent)
                && Objects.equals(element, that.element);
        }

        @Override
        public int hashCode() {
            return Objects.hash(parent, element);
        }

        @Override
        public String toString() {
            return element.toString();
        }
    }
}

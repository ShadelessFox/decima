package com.shade.decima.app.ui.tree;

import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * A concrete implementation of {@link javax.swing.tree.TreeModel} that uses
 * a {@link TreeStructure} to populate the tree.
 *
 * @param <T> the type of the elements in the tree
 * @see TreeStructure
 */
public final class StructuredTreeModel<T> extends AbstractTreeModel {
    private static final Logger log = LoggerFactory.getLogger(StructuredTreeModel.class);
    private final TreeStructure<T> structure;
    private Node<T> root;

    public StructuredTreeModel(@NotNull TreeStructure<T> structure) {
        this.structure = structure;
    }

    @Override
    public Object getRoot() {
        return getRootNode();
    }

    @Override
    public Object getChild(Object parent, int index) {
        Node<T> node = unwrap(parent);
        return getChildNodes(node).get(index);
    }

    @Override
    public int getChildCount(Object parent) {
        Node<T> node = unwrap(parent);
        return getChildNodes(node).size();
    }

    @Override
    public boolean isLeaf(Object parent) {
        Node<T> node = unwrap(parent);
        return !structure.hasChildren(node.element);
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        Node<T> node = unwrap(parent);
        return getChildNodes(node).indexOf((Node<?>) child);
    }

    @Override
    public void unload(@NotNull Object parent) {
        Node<T> node = unwrap(parent);
        node.children = null;
    }

    @SuppressWarnings("unchecked")
    private Node<T> unwrap(Object node) {
        return (Node<T>) node;
    }

    @NotNull
    private Object getRootNode() {
        if (root == null) {
            root = computeRootNode();
        }
        return root;
    }

    @NotNull
    private List<Node<T>> getChildNodes(Node<T> parent) {
        if (isLeaf(parent)) {
            return List.of();
        }
        if (parent.children == null) {
            parent.children = computeChildrenNodes(parent);
        }
        return parent.children;
    }

    @NotNull
    private Node<T> computeRootNode() {
        log.debug("Computing root for {}", structure);
        return new Node<>(structure.getRoot());
    }

    @NotNull
    private List<Node<T>> computeChildrenNodes(@NotNull Node<T> parent) {
        log.debug("Computing children of {} for {}", parent.element, structure);
        return structure.getChildren(parent.element).stream()
            .map(Node<T>::new)
            .toList();
    }

    private static class Node<T> implements TreeItem<T> {
        private final T element;
        private List<Node<T>> children;

        Node(T element) {
            this.element = element;
        }

        @NotNull
        @Override
        public T getValue() {
            return element;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Node<?> node
                && Objects.equals(element, node.element);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(element);
        }

        @Override
        public String toString() {
            return element.toString();
        }
    }
}

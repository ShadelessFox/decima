package com.shade.decima.hfw.ui.editor.tree;

import com.shade.platform.ui.controls.tree.TreeNodeWrapper;
import com.shade.util.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TreeStructureModel<T extends TreeStructure> extends AbstractTreeModel {
    private final T structure;
    private Node root;

    public TreeStructureModel(@NotNull T structure) {
        this.structure = structure;
    }

    @Override
    public Object getRoot() {
        return getRootNode();
    }

    @Override
    public Object getChild(Object parent, int index) {
        final Node node = (Node) parent;
        return getChildNodes(node).get(index);
    }

    @Override
    public int getChildCount(Object parent) {
        final Node node = (Node) parent;
        return getChildNodes(node).size();
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        final Node node = (Node) parent;
        return getChildNodes(node).indexOf((Node) child);
    }

    @Override
    public boolean isLeaf(Object element) {
        final Node node = (Node) element;
        return !structure.hasChildren(node.element);
    }

    @NotNull
    private Node getRootNode() {
        if (root == null) {
            root = new Node(structure.getRoot());
        }
        return root;
    }

    @NotNull
    private List<Node> getChildNodes(@NotNull Node parent) {
        if (!structure.hasChildren(parent.element)) {
            return List.of();
        }
        if (parent.children == null) {
            parent.children = new ArrayList<>();
            for (Object child : structure.getChildren(parent.element)) {
                parent.children.add(new Node(child));
            }
        }
        return parent.children;
    }

    private static class Node implements TreeNodeWrapper {
        private final Object element;
        private List<Node> children;

        public Node(@NotNull Object element) {
            this.element = element;
        }

        @NotNull
        @Override
        public Object getNode() {
            return element;
        }

        @Override
        public String toString() {
            return element.toString();
        }
    }
}

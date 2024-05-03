package com.shade.decima.hfw.ui.editor.tree;

import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.util.NotNull;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractTreeModel implements TreeModel {
    protected final List<TreeModelListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    public TreeNode[] getPathToRoot(@NotNull TreeNode root, @NotNull TreeNode node) {
        return getPathToRoot(root, node, 0);
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
}

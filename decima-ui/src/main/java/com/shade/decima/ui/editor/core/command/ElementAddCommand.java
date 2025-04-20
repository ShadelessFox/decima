package com.shade.decima.ui.editor.core.command;

import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.util.NotNull;

import javax.swing.tree.TreePath;

public class ElementAddCommand extends BaseNodeCommand {
    public enum Operation {
        ADD,
        REMOVE
    }

    private final Operation operation;
    private final Object value;
    private final int index;

    public ElementAddCommand(@NotNull Operation operation, @NotNull Tree tree, @NotNull CoreNodeObject node, @NotNull Object value, int index) {
        super(tree, node);
        this.operation = operation;
        this.value = value;
        this.index = index;
    }

    @Override
    public void redo() {
        super.redo();

        perform(operation == Operation.ADD);
    }

    @Override
    public void undo() {
        super.undo();

        perform(operation == Operation.REMOVE);
    }

    @NotNull
    @Override
    protected String getTitle() {
        return operation == Operation.ADD ? "Add new element" : "Remove element";
    }

    private void perform(boolean redo) {
        final Object value;
        final int leaf;

        if (redo) {
            value = getType().insert(node.getValue(), index, this.value);
            leaf = index;
        } else {
            value = getType().remove(node.getValue(), index);
            leaf = Math.max(0, Math.min(index, getType().length(value) - 1));
        }

        node.setValue(value);
        node.unloadChildren();
        tree.getModel().fireStructureChanged(node);

        final TreeNode child = tree.getModel().getChild(node, leaf);

        if (redo && child instanceof CoreNodeObject object) {
            object.setState(CoreNodeObject.State.NEW);
        }

        final TreePath path = tree.getModel().getTreePathToRoot(child);
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private RTTITypeArray<Object> getType() {
        return (RTTITypeArray<Object>) node.getType();
    }
}

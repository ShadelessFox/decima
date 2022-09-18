package com.shade.decima.ui.editor.core.command;

import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.platform.ui.commands.BaseCommand;
import com.shade.platform.ui.commands.Command;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.tree.TreePath;

public class AttributeChangeCommand extends BaseCommand {
    private final Tree tree;
    private final CoreNodeObject node;
    private final Object oldValue;
    private final Object newValue;

    public AttributeChangeCommand(@NotNull Tree tree, @NotNull CoreNodeObject node, @NotNull Object oldValue, @NotNull Object newValue) {
        this.tree = tree;
        this.node = node;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public void redo() {
        super.redo();

        node.setObject(newValue);
        tree.getModel().fireNodesChanged(node);

        final TreePath path = new TreePath(tree.getModel().getPathToRoot(node));
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
    }

    @Override
    public void undo() {
        super.undo();

        node.setObject(oldValue);
        tree.getModel().fireNodesChanged(node);

        final TreePath path = new TreePath(tree.getModel().getPathToRoot(node));
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
    }

    @Nullable
    @Override
    public Command merge(@NotNull Command other) {
        if (other instanceof AttributeChangeCommand c && c.node == node) {
            return other;
        } else {
            return this;
        }
    }

    @NotNull
    @Override
    public String getTitle() {
        return "Change '%s'".formatted(node.getLabel());
    }

    @NotNull
    public CoreNodeObject getNode() {
        return node;
    }

    @NotNull
    public Object getOldValue() {
        return oldValue;
    }

    @NotNull
    public Object getNewValue() {
        return newValue;
    }

    @Override
    public String toString() {
        return "PropertyChangeCommand{node=" + node.getLabel() + ", oldValue=" + oldValue + ", newValue=" + newValue + '}';
    }
}

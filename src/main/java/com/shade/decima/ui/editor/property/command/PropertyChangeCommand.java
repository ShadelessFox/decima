package com.shade.decima.ui.editor.property.command;

import com.shade.decima.ui.editor.property.PropertyObjectNode;
import com.shade.platform.ui.commands.BaseCommand;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.util.NotNull;

import javax.swing.tree.TreePath;

public class PropertyChangeCommand extends BaseCommand {
    private final Tree tree;
    private final PropertyObjectNode node;
    private final Object oldValue;
    private final Object newValue;

    public PropertyChangeCommand(@NotNull Tree tree, @NotNull PropertyObjectNode node, @NotNull Object oldValue, @NotNull Object newValue) {
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

    @NotNull
    @Override
    public String getTitle() {
        return "Change '%s'".formatted(node.getLabel());
    }
}

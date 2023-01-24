package com.shade.decima.ui.editor.core.command;

import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.util.NotNull;

import javax.swing.tree.TreePath;

public class ValueChangeCommand extends BaseNodeCommand {
    private final Object oldValue;
    private final Object newValue;

    public ValueChangeCommand(@NotNull Tree tree, @NotNull CoreNodeObject node, @NotNull Object oldValue, @NotNull Object newValue) {
        super(tree, node);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public void redo() {
        super.redo();

        node.setValue(newValue);
        tree.getModel().fireNodesChanged(node);

        final TreePath path = tree.getModel().getTreePathToRoot(node);
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
    }

    @Override
    public void undo() {
        super.undo();

        node.setValue(oldValue);
        tree.getModel().fireNodesChanged(node);

        final TreePath path = tree.getModel().getTreePathToRoot(node);
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
    }

    @NotNull
    @Override
    public String getTitle() {
        return "Change '%s'".formatted(node.getLabel());
    }
}

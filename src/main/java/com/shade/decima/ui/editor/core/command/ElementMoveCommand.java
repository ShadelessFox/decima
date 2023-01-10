package com.shade.decima.ui.editor.core.command;

import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.decima.ui.editor.core.CoreTree;
import com.shade.platform.ui.commands.BaseCommand;
import com.shade.util.NotNull;

import javax.swing.tree.TreePath;

public class ElementMoveCommand extends BaseCommand {
    private final CoreTree tree;
    private final CoreNodeObject node;
    private final int oldIndex;
    private final int newIndex;

    public ElementMoveCommand(@NotNull CoreTree tree, @NotNull CoreNodeObject node, int oldIndex, int newIndex) {
        this.tree = tree;
        this.node = node;
        this.oldIndex = oldIndex;
        this.newIndex = newIndex;
    }

    @Override
    public void redo() {
        super.redo();
        move(oldIndex, newIndex);
    }

    @Override
    public void undo() {
        super.undo();
        move(newIndex, oldIndex);
    }

    @NotNull
    @Override
    protected String getTitle() {
        return "Move element";
    }

    @SuppressWarnings("unchecked")
    private void move(int src, int dst) {
        final RTTITypeArray<Object> handler = (RTTITypeArray<Object>) node.getType();

        node.setValue(handler.move(node.getValue(), src, dst));
        node.unloadChildren();
        tree.getModel().fireStructureChanged(node);

        final TreePath path = tree.getModel().getTreePathToRoot(tree.getModel().getChild(node, dst));
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
    }
}

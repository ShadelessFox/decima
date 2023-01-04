package com.shade.decima.ui.editor.core.command;

import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.platform.ui.commands.BaseCommand;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.util.NotNull;

public class ElementRemoveCommand extends BaseCommand {
    private final Tree tree;
    private final CoreNodeObject node;
    private final Object value;
    private final int index;

    public ElementRemoveCommand(@NotNull Tree tree, @NotNull CoreNodeObject node, @NotNull Object value, int index) {
        this.tree = tree;
        this.node = node;
        this.value = value;
        this.index = index;
    }

    @Override
    public void redo() {
        super.redo();

        node.setValue(getType().remove(node.getValue(), index));
        node.unloadChildren();
        tree.getModel().fireStructureChanged(node);
    }

    @Override
    public void undo() {
        super.undo();

        node.setValue(getType().insert(node.getValue(), index, value));
        node.unloadChildren();
        tree.getModel().fireStructureChanged(node);
    }

    @NotNull
    @Override
    protected String getTitle() {
        return "Remove element";
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private RTTITypeArray<Object> getType() {
        return (RTTITypeArray<Object>) node.getType();
    }
}

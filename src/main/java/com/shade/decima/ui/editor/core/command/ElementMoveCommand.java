package com.shade.decima.ui.editor.core.command;

import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.decima.ui.editor.core.CoreTree;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.commands.BaseCommand;
import com.shade.util.NotNull;

import java.util.Objects;

public class ElementMoveCommand extends BaseCommand {
    private final CoreTree tree;
    private final CoreNodeObject child;
    private final int oldIndex;
    private final int newIndex;

    public ElementMoveCommand(@NotNull CoreTree tree, @NotNull CoreNodeObject child, int oldIndex, int newIndex) {
        this.tree = tree;
        this.child = child;
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
        final CoreNodeObject parent = (CoreNodeObject) Objects.requireNonNull(child.getParent());
        final RTTITypeArray<Object> handler = (RTTITypeArray<Object>) parent.getType();

        try {
            parent.setValue(handler.move(parent.getValue(), src, dst));
            parent.reloadChildren(new VoidProgressMonitor());
            tree.getModel().fireStructureChanged(parent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

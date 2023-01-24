package com.shade.decima.ui.editor.core.command;

import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.platform.ui.commands.BaseCommand;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.util.NotNull;

public abstract class BaseNodeCommand extends BaseCommand {
    protected final Tree tree;
    protected final CoreNodeObject node;
    protected final CoreNodeObject.State state;

    public BaseNodeCommand(@NotNull Tree tree, @NotNull CoreNodeObject node) {
        this.tree = tree;
        this.node = node;
        this.state = node.getState();
    }

    @Override
    public void redo() {
        super.redo();

        node.setState(CoreNodeObject.State.CHANGED);
    }

    @Override
    public void undo() {
        super.undo();

        node.setState(state);
    }

    @Override
    public void die() {
        super.die();

        node.setState(state);
    }
}

package com.shade.platform.ui.commands;

import com.shade.util.NotNull;

public abstract class BaseCommand implements Command {
    private State state;

    public BaseCommand() {
        this.state = State.UNDID;
    }

    @Override
    public void redo() {
        if (state != State.UNDID) {
            throw new IllegalStateException("Can't redo");
        }

        state = State.DID;
    }

    @Override
    public void undo() {
        if (state != State.DID) {
            throw new IllegalStateException("Can't undo");
        }

        state = State.UNDID;
    }

    @Override
    public void die() {
        state = State.DEAD;
    }

    @Override
    public boolean canRedo() {
        return state == State.UNDID;
    }

    @Override
    public boolean canUndo() {
        return state == State.DID;
    }

    @NotNull
    @Override
    public String getUndoTitle() {
        return getTitle();
    }

    @NotNull
    @Override
    public String getRedoTitle() {
        return getTitle();
    }

    @NotNull
    protected abstract String getTitle();

    enum State {
        DID,
        UNDID,
        DEAD
    }
}

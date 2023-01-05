package com.shade.platform.ui.commands;

import com.shade.util.NotNull;

public interface Command {
    void redo();

    void undo();

    boolean canRedo();

    boolean canUndo();

    @NotNull
    String getUndoTitle();

    @NotNull
    String getRedoTitle();
}

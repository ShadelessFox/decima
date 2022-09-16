package com.shade.platform.ui.commands;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

public interface Command {
    void redo();

    void undo();

    /**
     * Merges two commands together. The possible outcomes are following:
     *
     * <ul>
     *     <li>{@code this} if merge should not be performed</li>
     *     <li>{@code other} if {@code this} command should be skipped</li>
     *     <li>{@code null} if both {@code this} and {@code other} commands should be skipped</li>
     *     <li>{@link Command} if both {@code this} and {@code other} commands should be skipped and replaced by that command</li>
     * </ul>
     */
    @Nullable
    Command merge(@NotNull Command other);

    boolean canRedo();

    boolean canUndo();

    @NotNull
    String getUndoTitle();

    @NotNull
    String getRedoTitle();
}

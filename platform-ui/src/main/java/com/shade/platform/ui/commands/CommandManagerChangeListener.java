package com.shade.platform.ui.commands;

import com.shade.util.NotNull;

public interface CommandManagerChangeListener {
    default void commandDidRedo(@NotNull Command command) {
        // do nothing by default
    }

    default void commandDidUndo(@NotNull Command command) {
        // do nothing by default
    }
}

package com.shade.platform.ui.editors;

import com.shade.platform.ui.editors.stack.EditorStack;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.util.EventListener;

public interface EditorChangeListener extends EventListener {
    default void editorStackCreated(@NotNull EditorStack stack) {
        // do nothing by default
    }

    default void editorOpened(@NotNull Editor editor) {
        // do nothing by default
    }

    default void editorClosed(@NotNull Editor editor) {
        // do nothing by default
    }

    default void editorChanged(@Nullable Editor editor) {
        // do nothing by default
    }
}

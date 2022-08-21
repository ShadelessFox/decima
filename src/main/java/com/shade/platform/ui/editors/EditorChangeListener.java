package com.shade.platform.ui.editors;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

public interface EditorChangeListener {
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

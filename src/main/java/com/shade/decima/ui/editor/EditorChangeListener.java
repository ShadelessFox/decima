package com.shade.decima.ui.editor;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

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

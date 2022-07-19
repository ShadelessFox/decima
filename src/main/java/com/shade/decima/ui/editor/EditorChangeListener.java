package com.shade.decima.ui.editor;

import com.shade.decima.model.util.NotNull;

public interface EditorChangeListener {
    void editorOpened(@NotNull Editor editor);

    void editorClosed(@NotNull Editor editor);
}

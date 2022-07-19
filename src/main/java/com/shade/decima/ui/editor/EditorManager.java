package com.shade.decima.ui.editor;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

public interface EditorManager {
    @Nullable
    Editor findEditor(@NotNull EditorInput input);

    @NotNull
    Editor openEditor(@NotNull EditorInput input, boolean focus);

    @Nullable
    Editor getActiveEditor();

    @NotNull
    Editor[] getEditors();

    void closeEditor(@NotNull Editor editor);

    void addEditorChangeListener(@NotNull EditorChangeListener listener);

    void removeEditorChangeListener(@NotNull EditorChangeListener listener);
}

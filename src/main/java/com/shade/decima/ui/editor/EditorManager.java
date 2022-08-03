package com.shade.decima.ui.editor;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

public interface EditorManager {
    @Nullable
    Editor findEditor(@NotNull EditorInput input);

    @NotNull
    Editor openEditor(@NotNull EditorInput input, boolean focus);

    @NotNull
    Editor openEditor(@NotNull EditorInput input, boolean select, boolean focus);

    @Nullable
    Editor getActiveEditor();

    @NotNull
    Editor[] getEditors();

    int getEditorsCount();

    void closeEditor(@NotNull Editor editor);

    void addEditorChangeListener(@NotNull EditorChangeListener listener);

    void removeEditorChangeListener(@NotNull EditorChangeListener listener);
}

package com.shade.decima.ui.editor;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.editor.stack.EditorStack;

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

    @NotNull
    Editor[] getEditors(@NotNull EditorStack stack);

    int getEditorsCount();

    int getEditorsCount(@NotNull EditorStack stack);

    void closeEditor(@NotNull Editor editor);

    int getStacksCount();

    void addEditorChangeListener(@NotNull EditorChangeListener listener);

    void removeEditorChangeListener(@NotNull EditorChangeListener listener);
}

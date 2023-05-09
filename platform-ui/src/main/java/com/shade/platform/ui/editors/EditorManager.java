package com.shade.platform.ui.editors;

import com.shade.platform.ui.editors.stack.EditorStack;
import com.shade.platform.ui.editors.stack.EditorStackContainer;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

public interface EditorManager {
    @Nullable
    Editor findEditor(@NotNull EditorInput input);

    @NotNull
    Editor openEditor(@NotNull EditorInput input, boolean focus);

    @NotNull
    Editor openEditor(@NotNull EditorInput input, @Nullable EditorProvider provider, @Nullable EditorStack stack, boolean select, boolean focus);

    @NotNull
    Editor openEditor(@NotNull EditorInput input, @Nullable EditorProvider provider, @Nullable EditorStack stack, boolean select, boolean focus, int index);

    @Nullable
    Editor reuseEditor(@NotNull Editor oldEditor, @NotNull EditorInput newInput);

    @Nullable
    Editor getActiveEditor();

    @NotNull
    Editor[] getEditors();

    @NotNull
    Editor[] getEditors(@NotNull EditorStack stack);

    @NotNull
    Editor[] getRecentEditors();

    @NotNull
    EditorStackContainer getContainer();

    int getEditorsCount();

    int getEditorsCount(@NotNull EditorStack stack);

    void closeEditor(@NotNull Editor editor);

    void notifyInputChanged(@NotNull EditorInput input);

    int getStacksCount();

    void addEditorChangeListener(@NotNull EditorChangeListener listener);

    void removeEditorChangeListener(@NotNull EditorChangeListener listener);
}

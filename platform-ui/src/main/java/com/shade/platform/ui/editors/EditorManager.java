package com.shade.platform.ui.editors;

import com.shade.platform.model.app.ApplicationManager;
import com.shade.platform.model.messages.Topic;
import com.shade.platform.ui.editors.stack.EditorStack;
import com.shade.platform.ui.editors.stack.EditorStackContainer;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.awt.*;

public interface EditorManager {
    Topic<EditorChangeListener> EDITORS = Topic.create("editors", EditorChangeListener.class);

    @NotNull
    static EditorManager getInstance() {
        return ApplicationManager.getApplication().getService(EditorManager.class);
    }

    @Nullable
    Editor findEditor(@NotNull EditorInput input);

    @Nullable
    Editor findEditor(@NotNull Component c);

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
}

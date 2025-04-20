package com.shade.decima.ui.editor;

import com.shade.decima.model.app.Project;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.util.NotNull;

public interface ProjectEditorInput extends EditorInput {
    @NotNull
    Project getProject();
}

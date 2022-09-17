package com.shade.decima.ui.editor;

import com.shade.decima.model.app.Project;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.util.NotNull;

public interface FileEditorInput extends EditorInput {
    @NotNull
    NavigatorFileNode getNode();

    @NotNull
    Project getProject();
}

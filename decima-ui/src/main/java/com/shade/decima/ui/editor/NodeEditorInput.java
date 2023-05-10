package com.shade.decima.ui.editor;

import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.util.NotNull;

public interface NodeEditorInput extends ProjectEditorInput {
    @NotNull
    NavigatorFileNode getNode();
}

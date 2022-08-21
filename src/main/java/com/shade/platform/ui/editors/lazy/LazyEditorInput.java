package com.shade.platform.ui.editors.lazy;

import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.util.NotNull;

public interface LazyEditorInput extends EditorInput {
    @NotNull
    EditorInput loadRealInput(@NotNull ProgressMonitor monitor) throws Exception;
}

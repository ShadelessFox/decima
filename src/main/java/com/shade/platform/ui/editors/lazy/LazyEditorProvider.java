package com.shade.platform.ui.editors.lazy;

import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.platform.ui.editors.EditorProvider;
import com.shade.util.NotNull;

public class LazyEditorProvider implements EditorProvider {
    @NotNull
    @Override
    public Editor createEditor(@NotNull EditorInput input) {
        return new LazyEditor((LazyEditorInput) input);
    }

    @Override
    public boolean supports(@NotNull EditorInput input) {
        return input instanceof LazyEditorInput;
    }
}

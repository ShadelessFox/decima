package com.shade.platform.ui.editors;

import com.shade.util.NotNull;

public interface EditorProvider {
    @NotNull
    Editor createEditor(@NotNull EditorInput input);

    boolean supports(@NotNull EditorInput input);
}

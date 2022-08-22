package com.shade.platform.ui.editors;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

public interface EditorProvider {
    @NotNull
    Editor createEditor(@NotNull EditorInput input);

    boolean supports(@NotNull EditorInput input);

    @NotNull
    String getName();

    @Nullable
    Icon getIcon();
}

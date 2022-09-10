package com.shade.platform.ui.editors;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

public interface EditorInput {
    @NotNull
    String getName();

    @Nullable
    String getDescription();

    @Nullable
    Icon getIcon();

    boolean representsSameResource(@NotNull EditorInput other);
}

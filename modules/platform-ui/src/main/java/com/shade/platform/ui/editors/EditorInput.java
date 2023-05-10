package com.shade.platform.ui.editors;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

public interface EditorInput {
    @NotNull
    String getName();

    @Nullable
    String getDescription();

    boolean representsSameResource(@NotNull EditorInput other);
}

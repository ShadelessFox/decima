package com.shade.platform.ui.editors;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

public interface EditorProvider {
    @NotNull
    Editor createEditor(@NotNull EditorInput input);

    @NotNull
    Match matches(@NotNull EditorInput input);

    @NotNull
    String getName();

    @Nullable
    Icon getIcon();

    enum Match {
        /** The provider is determined to handle the supplied input */
        PRIMARY,
        /** The provider can handle the supplied input, but only if no providers with {@link Match#PRIMARY} was found */
        APPLIES,
        /** The provider can't handle the supplied input */
        NONE
    }
}

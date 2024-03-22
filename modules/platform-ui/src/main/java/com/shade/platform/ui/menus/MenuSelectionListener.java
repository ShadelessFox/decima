package com.shade.platform.ui.menus;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

public interface MenuSelectionListener {
    void selectionChanged(@NotNull MenuItem item, @NotNull MenuItemRegistration registration, @Nullable MenuItemContext context);

    void selectionCleared();
}

package com.shade.decima.ui.data;

import com.shade.util.NotNull;

public interface ValueManager<T> {
    @NotNull
    ValueEditor<T> createEditor(@NotNull ValueController<T> controller);

    boolean canEdit(@NotNull ValueController.EditType type);
}

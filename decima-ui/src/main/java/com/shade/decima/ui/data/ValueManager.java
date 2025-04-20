package com.shade.decima.ui.data;

import com.shade.util.NotNull;

public interface ValueManager<T> {
    @NotNull
    ValueEditor<T> createEditor(@NotNull MutableValueController<T> controller);

    boolean canEdit(@NotNull MutableValueController.EditType type);
}

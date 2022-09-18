package com.shade.decima.ui.data;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

public interface ValueManager<T> {
    @Nullable
    ValueEditor<T> createEditor(@NotNull ValueController<T> controller);
}

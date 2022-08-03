package com.shade.decima.ui.menu;

import com.shade.decima.model.app.DataContext;
import com.shade.decima.model.app.DataKey;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

public record MenuItemContext(@NotNull DataContext context, @Nullable Object source) implements DataContext {
    @Override
    public Object getData(@NotNull String key) {
        return context.getData(key);
    }

    @Override
    public <T> T getData(@NotNull DataKey<T> key) {
        return context.getData(key);
    }
}

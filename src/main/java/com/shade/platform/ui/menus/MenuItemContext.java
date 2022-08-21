package com.shade.platform.ui.menus;

import com.shade.platform.model.data.DataContext;
import com.shade.platform.model.data.DataKey;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

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

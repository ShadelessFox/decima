package com.shade.platform.ui.menus;

import com.shade.platform.model.data.DataContext;
import com.shade.platform.model.data.DataKey;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;

public record MenuItemContext(@NotNull DataContext context, @Nullable Object source, @Nullable ActionEvent event) implements DataContext {
    @Override
    public Object getData(@NotNull String key) {
        return context.getData(key);
    }

    @Override
    public <T> T getData(@NotNull DataKey<T> key) {
        return context.getData(key);
    }

    public boolean isLeftMouseButton() {
        return event != null && (event.getModifiers() & InputEvent.BUTTON1_MASK) != 0;
    }

    public boolean isRightMouseButton() {
        return event != null && (event.getModifiers() & InputEvent.BUTTON3_MASK) != 0;
    }

    @NotNull
    public MenuItemContext withEvent(@NotNull ActionEvent event) {
        return new MenuItemContext(context, source, event);
    }
}

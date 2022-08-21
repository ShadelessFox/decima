package com.shade.platform.ui.menus;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

public abstract class MenuItem extends Menu {
    public abstract void perform(@NotNull MenuItemContext ctx);

    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        return isVisible(ctx);
    }

    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return true;
    }

    public boolean isChecked(@NotNull MenuItemContext ctx) {
        return false;
    }

    @Nullable
    public String getName(@NotNull MenuItemContext ctx) {
        return null;
    }

    @Nullable
    public String getIcon(@NotNull MenuItemContext ctx) {
        return null;
    }
}

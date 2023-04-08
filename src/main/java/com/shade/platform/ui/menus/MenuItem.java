package com.shade.platform.ui.menus;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

public abstract class MenuItem extends Menu {
    public void perform(@NotNull MenuItemContext ctx) {
        // do nothing by default so menus with children may not override this method
    }

    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        return isVisible(ctx);
    }

    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return true;
    }

    @Nullable
    public String getName(@NotNull MenuItemContext ctx) {
        return null;
    }

    @Nullable
    public Icon getIcon(@NotNull MenuItemContext ctx) {
        return null;
    }

    public interface Check {
        boolean isChecked(@NotNull MenuItemContext ctx);
    }

    public interface Radio {
        boolean isSelected(@NotNull MenuItemContext ctx);
    }
}

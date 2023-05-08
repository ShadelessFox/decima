package com.shade.platform.ui.menus;

import com.shade.platform.model.data.DataContext;
import com.shade.util.NotNull;

import javax.swing.*;

public interface MenuManager {
    String CTX_MENU_ID = "menu.ctx";
    String APP_MENU_ID = "menu.app";
    String BAR_MENU_ID = "menu.bar";

    @NotNull
    JToolBar createToolBar(@NotNull JComponent component, @NotNull String id, @NotNull DataContext context);

    void installContextMenu(@NotNull JComponent component, @NotNull String id, @NotNull DataContext context);

    void installMenuBar(@NotNull JRootPane pane, @NotNull String id, @NotNull DataContext context);

    void update(@NotNull JToolBar toolBar);
}

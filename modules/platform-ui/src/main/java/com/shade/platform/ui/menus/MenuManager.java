package com.shade.platform.ui.menus;

import com.shade.platform.model.app.ApplicationManager;
import com.shade.platform.model.data.DataContext;
import com.shade.platform.model.data.DataKey;
import com.shade.platform.model.messages.Topic;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

public interface MenuManager {
    Topic<MenuSelectionListener> SELECTION = Topic.create("menu selection", MenuSelectionListener.class);

    DataKey<DataContext> CONTEXT_KEY = new DataKey<>("context", DataContext.class);

    String CTX_MENU_ID = "menu.ctx";
    String APP_MENU_ID = "menu.app";
    String BAR_MENU_ID = "menu.bar";

    @NotNull
    static MenuManager getInstance() {
        return ApplicationManager.getApplication().getService(MenuManager.class);
    }

    @NotNull
    JToolBar createToolBar(@NotNull JComponent component, @NotNull String id, @NotNull DataContext context);

    @NotNull
    JPopupMenu createPopupMenu(@NotNull JComponent component, @NotNull String id, @NotNull DataContext context);

    void installContextMenu(@NotNull JComponent component, @NotNull String id, @NotNull DataContext context);

    void installMenuBar(@NotNull JRootPane pane, @NotNull String id, @NotNull DataContext context);

    void update(@NotNull JToolBar toolBar);

    @Nullable
    MenuItemRegistration findItem(@NotNull String id);
}

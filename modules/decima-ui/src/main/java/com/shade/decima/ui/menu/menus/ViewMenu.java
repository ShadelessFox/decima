package com.shade.decima.ui.menu.menus;

import com.shade.decima.ui.dialogs.RecentEditorsDialog;
import com.shade.platform.model.LazyWithMetadata;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.menus.*;
import com.shade.platform.ui.views.ViewManager;
import com.shade.util.NotNull;

import javax.swing.*;
import java.util.List;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuRegistration(id = APP_MENU_VIEW_ID, name = "&View", order = 3000)
public final class ViewMenu extends Menu {
    @MenuItemRegistration(parent = APP_MENU_VIEW_ID, id = APP_MENU_VIEW_TOOL_WINDOWS_ID, name = "Tool Windows", group = APP_MENU_VIEW_GROUP_APPEARANCE, order = 1000)
    public static class ToolWindowsItem extends MenuItem {}

    @MenuItemRegistration(parent = APP_MENU_VIEW_TOOL_WINDOWS_ID, group = APP_MENU_VIEW_TOOL_WINDOWS_GROUP_GENERAL, order = 1000)
    public static class ToolWindowPlaceholderItem extends MenuItem implements MenuItemProvider {
        @NotNull
        @Override
        public List<LazyWithMetadata<MenuItem, MenuItemRegistration>> create(@NotNull MenuItemContext ctx) {
            return ViewManager.getInstance().getViews().stream()
                .map(c -> LazyWithMetadata.of(
                    () -> (MenuItem) new ToolWindowItem(c.metadata().id()),
                    MenuItemProvider.createRegistration(
                        APP_MENU_VIEW_TOOL_WINDOWS_ID,
                        APP_MENU_VIEW_TOOL_WINDOWS_GROUP_GENERAL,
                        c.metadata().label(),
                        c.metadata().icon(),
                        c.metadata().keystroke()
                    ),
                    ToolWindowItem.class
                ))
                .toList();
        }
    }

    public static class ToolWindowItem extends MenuItem {
        private final String id;

        public ToolWindowItem(@NotNull String id) {
            this.id = id;
        }

        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final ViewManager manager = ViewManager.getInstance();

            if (manager.isShowing(id, ctx.source() != null)) {
                manager.hideView(id);
            } else {
                manager.showView(id);
            }
        }
    }

    @MenuItemRegistration(id = RecentFilesItem.ID, parent = APP_MENU_VIEW_ID, name = "Rec&ent Editors", keystroke = "ctrl E", group = APP_MENU_VIEW_GROUP_GENERAL, order = 1000)
    public static class RecentFilesItem extends MenuItem {
        public static final String ID = APP_MENU_VIEW_ID + ".recentEditors";

        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            new RecentEditorsDialog(JOptionPane.getRootFrame());
        }

        @Override
        public boolean isEnabled(@NotNull MenuItemContext ctx) {
            return EditorManager.getInstance().getEditorsCount() > 0;
        }
    }
}

package com.shade.decima.ui.menu.menus;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.dialogs.RecentEditorsDialog;
import com.shade.platform.model.LazyWithMetadata;
import com.shade.platform.ui.menus.*;
import com.shade.platform.ui.views.ViewManager;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.prefs.Preferences;

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
            return Application.getViewManager().getViews().stream()
                .map(c -> LazyWithMetadata.of(
                    () -> (MenuItem) new ToolWindowItem(c.metadata().id()),
                    MenuItemProvider.createRegistration(
                        APP_MENU_VIEW_TOOL_WINDOWS_ID,
                        APP_MENU_VIEW_TOOL_WINDOWS_GROUP_GENERAL,
                        c.metadata().label(),
                        c.metadata().icon(),
                        c.metadata().keystroke()
                    )
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
            final ViewManager manager = Application.getViewManager();

            if (manager.isShowing(id, ctx.source() != null)) {
                manager.hideView(id);
            } else {
                manager.showView(id);
            }
        }
    }

    @MenuItemRegistration(parent = APP_MENU_VIEW_ID, id = APP_MENU_VIEW_THEME_ID, name = "&Theme", group = APP_MENU_VIEW_GROUP_APPEARANCE, order = 2000)
    public static class ThemeItem extends MenuItem {}

    @MenuItemRegistration(parent = APP_MENU_VIEW_THEME_ID, group = APP_MENU_VIEW_THEME_GROUP_GENERAL, order = 1000)
    public static class ThemePlaceholderItem extends MenuItem implements MenuItemProvider {
        private static final MenuItemRegistration REGISTRATION = MenuItemProvider.createRegistration(APP_MENU_VIEW_THEME_ID, APP_MENU_VIEW_THEME_GROUP_GENERAL);

        private static final List<ThemeInfo> THEMES = List.of(
            new ThemeInfo("Light", FlatLightLaf.class.getName()),
            new ThemeInfo("Dark", FlatDarkLaf.class.getName())
        );

        @NotNull
        @Override
        public List<LazyWithMetadata<MenuItem, MenuItemRegistration>> create(@NotNull MenuItemContext ctx) {
            return THEMES.stream()
                .map(theme -> LazyWithMetadata.of(() -> (MenuItem) new ChangeThemeItem(theme), REGISTRATION))
                .toList();
        }
    }

    public static class ChangeThemeItem extends MenuItem implements MenuItem.Radio {
        private final ThemeInfo info;

        public ChangeThemeItem(@NotNull ThemeInfo info) {
            this.info = info;
        }

        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            Application.getWorkspace().getPreferences().node("window").put("laf", info.className);

            JOptionPane.showMessageDialog(
                Application.getFrame(),
                "The theme will change upon application restart.",
                "Theme Change",
                JOptionPane.INFORMATION_MESSAGE);
        }

        @Nullable
        @Override
        public String getName(@NotNull MenuItemContext ctx) {
            return info.name;
        }

        @Override
        public boolean isSelected(@NotNull MenuItemContext ctx) {
            final Preferences prefs = Application.getWorkspace().getPreferences();
            final String laf = prefs.node("window").get("laf", FlatLightLaf.class.getName());
            return laf.equals(info.className);
        }
    }

    public static record ThemeInfo(@NotNull String name, @NotNull String className) {}

    @MenuItemRegistration(parent = APP_MENU_VIEW_ID, name = "Rec&ent Editors", keystroke = "ctrl E", group = APP_MENU_VIEW_GROUP_GENERAL, order = 1000)
    public static class RecentFilesItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            new RecentEditorsDialog(Application.getFrame());
        }

        @Override
        public boolean isEnabled(@NotNull MenuItemContext ctx) {
            return Application.getEditorManager().getEditorsCount() > 0;
        }
    }
}

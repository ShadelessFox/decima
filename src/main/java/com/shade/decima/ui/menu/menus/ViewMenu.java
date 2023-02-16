package com.shade.decima.ui.menu.menus;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.shade.decima.ui.Application;
import com.shade.platform.model.LazyWithMetadata;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemProvider;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.prefs.Preferences;

import static com.shade.decima.ui.menu.MenuConstants.*;

public interface ViewMenu {
    @MenuItemRegistration(parent = APP_MENU_VIEW_ID, id = APP_MENU_VIEW_THEME_ID, name = "&Theme", group = APP_MENU_VIEW_GROUP_GENERAL, order = 1000)
    class ThemeItem extends MenuItem {}

    @MenuItemRegistration(parent = APP_MENU_VIEW_THEME_ID, group = APP_MENU_VIEW_THEME_GROUP_GENERAL, order = 1000)
    class ThemePlaceholderItem extends MenuItem implements MenuItemProvider {
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

    class ChangeThemeItem extends MenuItem {
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
        public boolean isChecked(@NotNull MenuItemContext ctx) {
            final Preferences prefs = Application.getWorkspace().getPreferences();
            final String laf = prefs.node("window").get("laf", FlatLightLaf.class.getName());
            return laf.equals(info.className);
        }

        @Override
        public boolean isRadio(@NotNull MenuItemContext ctx) {
            return true;
        }
    }

    record ThemeInfo(@NotNull String name, @NotNull String className) {}
}

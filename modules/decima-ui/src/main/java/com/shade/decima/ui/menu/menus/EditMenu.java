package com.shade.decima.ui.menu.menus;

import com.shade.decima.model.app.Project;
import com.shade.decima.ui.CommonDataKeys;
import com.shade.decima.ui.dialogs.FindFilesDialog;
import com.shade.platform.ui.commands.CommandManager;
import com.shade.platform.ui.menus.*;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuRegistration(id = APP_MENU_EDIT_ID, name = "&Edit", order = 2000)
public final class EditMenu extends Menu {
    @MenuItemRegistration(parent = APP_MENU_EDIT_ID, icon = "Action.undoIcon", keystroke = "ctrl Z", group = APP_MENU_EDIT_GROUP_UNDO, order = 1000)
    public static class UndoItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final CommandManager manager = ctx.getData(CommonDataKeys.COMMAND_MANAGER_KEY);
            if (manager != null) {
                manager.undo();
            }
        }

        @Override
        public boolean isEnabled(@NotNull MenuItemContext ctx) {
            final CommandManager manager = ctx.getData(CommonDataKeys.COMMAND_MANAGER_KEY);
            return manager != null && manager.canUndo();
        }

        @Override
        public boolean isVisible(@NotNull MenuItemContext ctx) {
            return ctx.getData(CommonDataKeys.COMMAND_MANAGER_KEY) != null;
        }

        @Nullable
        @Override
        public String getName(@NotNull MenuItemContext ctx) {
            final CommandManager manager = ctx.getData(CommonDataKeys.COMMAND_MANAGER_KEY);
            if (manager != null && manager.canUndo()) {
                return "&Undo %s".formatted(manager.getUndoTitle());
            } else {
                return "&Undo";
            }
        }
    }

    @MenuItemRegistration(parent = APP_MENU_EDIT_ID, icon = "Action.redoIcon", keystroke = "ctrl shift Z", group = APP_MENU_EDIT_GROUP_UNDO, order = 2000)
    public static class RedoItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final CommandManager manager = ctx.getData(CommonDataKeys.COMMAND_MANAGER_KEY);
            if (manager != null) {
                manager.redo();
            }
        }

        @Override
        public boolean isEnabled(@NotNull MenuItemContext ctx) {
            final CommandManager manager = ctx.getData(CommonDataKeys.COMMAND_MANAGER_KEY);
            return manager != null && manager.canRedo();
        }

        @Override
        public boolean isVisible(@NotNull MenuItemContext ctx) {
            return ctx.getData(CommonDataKeys.COMMAND_MANAGER_KEY) != null;
        }

        @Nullable
        @Override
        public String getName(@NotNull MenuItemContext ctx) {
            final CommandManager manager = ctx.getData(CommonDataKeys.COMMAND_MANAGER_KEY);
            if (manager != null && manager.canRedo()) {
                return "&Redo %s".formatted(manager.getRedoTitle());
            } else {
                return "&Redo";
            }
        }
    }

    @MenuItemRegistration(id = FindFilesItem.ID, parent = APP_MENU_EDIT_ID, name = "Find &Files\u2026", icon = "Action.searchIcon", keystroke = "ctrl shift F", group = APP_MENU_EDIT_GROUP_GENERAL, order = 1000)
    public static class FindFilesItem extends MenuItem {
        public static final String ID = APP_MENU_EDIT_ID + ".findFiles";

        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final Project project = ctx.getData(CommonDataKeys.PROJECT_KEY);
            if (project != null) {
                FindFilesDialog.show(JOptionPane.getRootFrame(), project, FindFilesDialog.Strategy.FIND_MATCHING, null);
            }
        }

        @Override
        public boolean isEnabled(@NotNull MenuItemContext ctx) {
            return ctx.getData(CommonDataKeys.PROJECT_KEY) != null;
        }
    }
}

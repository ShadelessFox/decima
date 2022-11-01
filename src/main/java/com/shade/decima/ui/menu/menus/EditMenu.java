package com.shade.decima.ui.menu.menus;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.shade.decima.model.app.Project;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.ApplicationFrame;
import com.shade.decima.ui.dialogs.FindFileDialog;
import com.shade.decima.ui.editor.FileEditorInput;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.impl.NavigatorNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.ui.commands.CommandManager;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.SaveableEditor;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import static com.shade.decima.ui.menu.MenuConstants.*;

public interface EditMenu {
    @MenuItemRegistration(parent = APP_MENU_EDIT_ID, icon = "Editor.undoIcon", keystroke = "ctrl Z", group = APP_MENU_EDIT_GROUP_UNDO, order = 1000)
    class UndoItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final CommandManager manager = findActiveCommandManager();
            if (manager != null) {
                manager.undo();
            }
        }

        @Override
        public boolean isEnabled(@NotNull MenuItemContext ctx) {
            final CommandManager manager = findActiveCommandManager();
            return manager != null && manager.canUndo();
        }

        @Override
        public boolean isVisible(@NotNull MenuItemContext ctx) {
            return findActiveCommandManager() != null;
        }

        @Nullable
        @Override
        public String getName(@NotNull MenuItemContext ctx) {
            final CommandManager manager = findActiveCommandManager();
            if (manager != null && manager.canUndo()) {
                return "&Undo %s".formatted(manager.getUndoTitle());
            } else {
                return "&Undo";
            }
        }
    }

    @MenuItemRegistration(parent = APP_MENU_EDIT_ID, icon = "Editor.redoIcon", keystroke = "ctrl shift Z", group = APP_MENU_EDIT_GROUP_UNDO, order = 2000)
    class RedoItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final CommandManager manager = findActiveCommandManager();
            if (manager != null) {
                manager.redo();
            }
        }

        @Override
        public boolean isEnabled(@NotNull MenuItemContext ctx) {
            final CommandManager manager = findActiveCommandManager();
            return manager != null && manager.canRedo();
        }

        @Override
        public boolean isVisible(@NotNull MenuItemContext ctx) {
            return findActiveCommandManager() != null;
        }

        @Nullable
        @Override
        public String getName(@NotNull MenuItemContext ctx) {
            final CommandManager manager = findActiveCommandManager();
            if (manager != null && manager.canRedo()) {
                return "&Redo %s".formatted(manager.getRedoTitle());
            } else {
                return "&Redo";
            }
        }
    }

    @MenuItemRegistration(parent = APP_MENU_EDIT_ID, name = "Find &Files\u2026", icon = "Editor.searchIcon", keystroke = "ctrl shift F", group = APP_MENU_EDIT_GROUP_GENERAL, order = 1000)
    class FindFilesItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final Project project = findActiveProject();

            if (project == null) {
                return;
            }

            new FindFileDialog(Application.getFrame(), project).setVisible(true);
        }

        @Override
        public boolean isEnabled(@NotNull MenuItemContext ctx) {
            return findActiveProject() != null;
        }
    }

    @Nullable
    private static Project findActiveProject() {
        final ApplicationFrame frame = Application.getFrame();
        final NavigatorTree navigator = frame.getNavigator();

        // FIXME: We don't have the concept of an active context yet. It would be useful here
        //        That way we could access the global active context and retrieve whatever we
        //        might need from it

        if (FlatUIUtils.isPermanentFocusOwner(navigator) && navigator.getLastSelectedPathComponent() instanceof NavigatorNode node) {
            final NavigatorProjectNode root = node.getParentOfType(NavigatorProjectNode.class);

            if (!root.needsInitialization()) {
                return root.getProject();
            }
        }

        final Editor editor = frame.getEditorManager().getActiveEditor();

        if (editor != null && editor.getInput() instanceof FileEditorInput input) {
            return input.getProject();
        }

        return null;
    }

    @Nullable
    private static CommandManager findActiveCommandManager() {
        if (Application.getFrame().getEditorManager().getActiveEditor() instanceof SaveableEditor se) {
            return se.getCommandManager();
        } else {
            return null;
        }
    }
}

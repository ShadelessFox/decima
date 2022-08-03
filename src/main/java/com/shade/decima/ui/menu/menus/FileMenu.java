package com.shade.decima.ui.menu.menus;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.Workspace;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.dialogs.BaseEditDialog;
import com.shade.decima.ui.dialogs.ProjectEditDialog;
import com.shade.decima.ui.menu.MenuItem;
import com.shade.decima.ui.menu.MenuItemContext;
import com.shade.decima.ui.menu.MenuItemRegistration;

import java.nio.file.Path;
import java.util.UUID;

import static com.shade.decima.ui.menu.MenuConstants.*;

public interface FileMenu {
    @MenuItemRegistration(id = APP_MENU_FILE_NEW_ID, parent = APP_MENU_FILE_ID, name = "&New", group = APP_MENU_FILE_GROUP_OPEN, order = 1000)
    class NewItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
        }
    }

    @MenuItemRegistration(parent = APP_MENU_FILE_NEW_ID, name = "&Project\u2026", group = APP_MENU_FILE_GROUP_OPEN, order = 1000)
    class NewProjectItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final ProjectEditDialog dialog = new ProjectEditDialog(false);
            final ProjectContainer container = new ProjectContainer(UUID.randomUUID(), "New project", GameType.DS, Path.of(""), Path.of(""), Path.of(""), Path.of(""), Path.of(""));

            dialog.load(container);

            if (dialog.showDialog(Application.getFrame()) == BaseEditDialog.OK_ID) {
                final Workspace workspace = Application.getFrame().getWorkspace();
                dialog.save(container);
                workspace.addProject(container, true, true);
            }

        }
    }

    @MenuItemRegistration(parent = APP_MENU_FILE_ID, name = "E&xit", keystroke = "ctrl Q", group = APP_MENU_FILE_GROUP_EXIT, order = 1000)
    class ExitItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            Application.getFrame().dispose();
        }
    }
}

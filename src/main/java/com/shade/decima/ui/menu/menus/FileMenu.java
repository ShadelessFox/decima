package com.shade.decima.ui.menu.menus;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.base.GameType;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.dialogs.PersistChangesDialog;
import com.shade.decima.ui.dialogs.ProjectEditDialog;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.platform.ui.editors.SaveableEditor;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.nio.file.Path;
import java.util.UUID;

import static com.shade.decima.ui.menu.MenuConstants.*;

public interface FileMenu {
    @MenuItemRegistration(id = APP_MENU_FILE_NEW_ID, parent = APP_MENU_FILE_ID, name = "&New", group = APP_MENU_FILE_GROUP_OPEN, order = 1000)
    class NewItem extends MenuItem {}

    @MenuItemRegistration(parent = APP_MENU_FILE_NEW_ID, name = "&Project\u2026", group = APP_MENU_FILE_GROUP_OPEN, order = 1000)
    class NewProjectItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final ProjectEditDialog dialog = new ProjectEditDialog(false);
            final ProjectContainer container = new ProjectContainer(UUID.randomUUID(), "New project", GameType.values()[0], Path.of(""), Path.of(""), Path.of(""), Path.of(""), Path.of(""), Path.of(""));

            dialog.load(container);

            if (dialog.showDialog(Application.getFrame()) == BaseDialog.BUTTON_OK) {
                dialog.save(container);
                Application.getWorkspace().addProject(container, true, true);
            }
        }
    }

    @MenuItemRegistration(parent = APP_MENU_FILE_ID, name = "&Save", icon = "Action.saveIcon", keystroke = "ctrl S", group = APP_MENU_FILE_GROUP_SAVE, order = 1000)
    class SaveItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final SaveableEditor editor = findSaveableEditor();

            if (editor != null) {
                editor.doSave(new VoidProgressMonitor());
            }
        }

        @Override
        public boolean isEnabled(@NotNull MenuItemContext ctx) {
            final SaveableEditor editor = findSaveableEditor();
            return editor != null && editor.isDirty();
        }

        @Nullable
        private static SaveableEditor findSaveableEditor() {
            if (Application.getEditorManager().getActiveEditor() instanceof SaveableEditor se) {
                return se;
            } else {
                return null;
            }
        }
    }

    @MenuItemRegistration(parent = APP_MENU_FILE_ID, name = "Re&pack", icon = "Action.packIcon", keystroke = "ctrl P", group = APP_MENU_FILE_GROUP_SAVE, order = 2000)
    class RepackItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final Project project = UIUtils.findActiveProject();

            if (project != null) {
                final NavigatorTree navigator = Application.getNavigator();
                final NavigatorProjectNode root = navigator.getModel().getProjectNode(new VoidProgressMonitor(), project.getContainer());

                new PersistChangesDialog(root).showDialog(Application.getFrame());
            }
        }

        @Override
        public boolean isEnabled(@NotNull MenuItemContext ctx) {
            final Project project = UIUtils.findActiveProject();
            return project != null && project.getPackfileManager().hasChanges();
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

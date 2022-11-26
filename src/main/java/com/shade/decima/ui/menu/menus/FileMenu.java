package com.shade.decima.ui.menu.menus;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.Workspace;
import com.shade.decima.model.base.GameType;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.dialogs.PersistChangesDialog;
import com.shade.decima.ui.dialogs.ProjectEditDialog;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.impl.NavigatorNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.platform.ui.editors.SaveableEditor;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
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
            final ProjectContainer container = new ProjectContainer(UUID.randomUUID(), "New project", GameType.DS, Path.of(""), Path.of(""), Path.of(""), Path.of(""), Path.of(""), Path.of(""));

            dialog.load(container);

            if (dialog.showDialog(Application.getFrame()) == BaseDialog.BUTTON_OK) {
                final Workspace workspace = Application.getFrame().getWorkspace();
                dialog.save(container);
                workspace.addProject(container, true, true);
            }
        }
    }

    @MenuItemRegistration(parent = APP_MENU_FILE_ID, name = "&Save", icon = "Editor.saveIcon", keystroke = "ctrl S", group = APP_MENU_FILE_GROUP_SAVE, order = 1000)
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
            if (Application.getFrame().getEditorManager().getActiveEditor() instanceof SaveableEditor se) {
                return se;
            } else {
                return null;
            }
        }
    }

    @MenuItemRegistration(parent = APP_MENU_FILE_ID, name = "Re&pack", icon = "Editor.packIcon", keystroke = "ctrl P", group = APP_MENU_FILE_GROUP_SAVE, order = 2000)
    class RepackItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final NavigatorProjectNode root = findProjectNode();

            if (root != null) {
                new PersistChangesDialog(root).showDialog(Application.getFrame());
            }
        }

        @Override
        public boolean isEnabled(@NotNull MenuItemContext ctx) {
            final NavigatorProjectNode node = findProjectNode();
            return node != null && !node.needsInitialization() && node.getProject().getPersister().hasChanges();
        }

        @Nullable
        private static NavigatorProjectNode findProjectNode() {
            final NavigatorTree navigator = Application.getFrame().getNavigator();

            if (navigator.getLastSelectedPathComponent() instanceof NavigatorNode node) {
                final NavigatorProjectNode root = node.getParentOfType(NavigatorProjectNode.class);

                if (!root.needsInitialization()) {
                    return root;
                }
            }

            return null;
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

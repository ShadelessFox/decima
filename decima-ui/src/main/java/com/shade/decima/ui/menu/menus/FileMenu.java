package com.shade.decima.ui.menu.menus;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.ProjectManager;
import com.shade.decima.model.base.GameType;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.CommonDataKeys;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.dialogs.PersistChangesDialog;
import com.shade.decima.ui.dialogs.ProjectEditDialog;
import com.shade.decima.ui.editor.FileEditorInputSimple;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.NavigatorTreeModel;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.platform.ui.dialogs.ProgressDialog;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.editors.SaveableEditor;
import com.shade.platform.ui.menus.*;
import com.shade.platform.ui.settings.impl.SettingsDialog;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.UUID;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuRegistration(id = APP_MENU_FILE_ID, name = "&File", order = 1000)
public final class FileMenu extends Menu {
    @MenuItemRegistration(id = APP_MENU_FILE_NEW_ID, parent = APP_MENU_FILE_ID, name = "&New", group = APP_MENU_FILE_GROUP_OPEN, order = 1000)
    public static class NewItem extends MenuItem {}

    @MenuItemRegistration(id = NewProjectItem.ID, parent = APP_MENU_FILE_NEW_ID, name = "&Project\u2026", description = "Create a new project", keystroke = "ctrl N", group = APP_MENU_FILE_GROUP_OPEN, order = 1000)
    public static class NewProjectItem extends MenuItem {
        public static final String ID = APP_MENU_FILE_NEW_ID + ".project";

        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final ProjectEditDialog dialog = new ProjectEditDialog(false, true);
            final ProjectContainer container = new ProjectContainer(UUID.randomUUID(), "New project", GameType.values()[0], Path.of(""), Path.of(""), Path.of(""));

            dialog.load(container);

            if (dialog.showDialog(JOptionPane.getRootFrame()) == BaseDialog.BUTTON_OK) {
                dialog.save(container);
                ProjectManager.getInstance().addProject(container);
            }
        }
    }

    @MenuItemRegistration(parent = APP_MENU_FILE_ID, name = "&Open\u2026", description = "Open a file in editor", icon = "Tree.openIcon", group = APP_MENU_FILE_GROUP_OPEN, order = 2000)
    public static class OpenItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose input file");
            chooser.setFileFilter(new FileExtensionFilter("Decima Core File", "core"));
            chooser.setAcceptAllFileFilterUsed(false);

            if (chooser.showOpenDialog(JOptionPane.getRootFrame()) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            final ProjectContainer container = (ProjectContainer) JOptionPane.showInputDialog(
                JOptionPane.getRootFrame(),
                "Choose the project to associate the core file with:",
                "Choose project",
                JOptionPane.PLAIN_MESSAGE,
                null,
                ProjectManager.getInstance().getProjects(),
                null
            );

            if (container == null) {
                return;
            }

            ProgressDialog.showProgressDialog(JOptionPane.getRootFrame(), "Opening file", monitor -> {
                final NavigatorTreeModel model = Application.getNavigator().getModel();
                final NavigatorProjectNode node = model.getProjectNode(monitor, container);

                try (var ignored = monitor.begin("Open project")) {
                    node.open();
                    model.fireNodesChanged(node);
                } catch (IOException e) {
                    throw new UncheckedIOException("Unable to open the project", e);
                }

                return EditorManager.getInstance().openEditor(
                    new FileEditorInputSimple(chooser.getSelectedFile().toPath(), node.getProject()),
                    true
                );
            });
        }

        @Override
        public boolean isVisible(@NotNull MenuItemContext ctx) {
            return ProjectManager.getInstance().getProjects().length > 0;
        }
    }

    @MenuItemRegistration(parent = APP_MENU_FILE_ID, name = "&Save", icon = "Action.saveIcon", keystroke = "ctrl S", group = APP_MENU_FILE_GROUP_SAVE, order = 1000)
    public static class SaveItem extends MenuItem {
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
            if (EditorManager.getInstance().getActiveEditor() instanceof SaveableEditor se) {
                return se;
            } else {
                return null;
            }
        }
    }

    @MenuItemRegistration(parent = APP_MENU_FILE_ID, name = "Se&ttings\u2026", description = "Edit application settings", group = APP_MENU_FILE_GROUP_SETTINGS, keystroke = "ctrl alt S", order = 1000)
    public static class SettingsItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            new SettingsDialog().showDialog(JOptionPane.getRootFrame());
        }
    }

    @MenuItemRegistration(parent = APP_MENU_FILE_ID, name = "Re&pack", icon = "Action.packIcon", keystroke = "ctrl P", group = APP_MENU_FILE_GROUP_SAVE, order = 2000)
    public static class RepackItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final Project project = ctx.getData(CommonDataKeys.PROJECT_KEY);

            if (project != null) {
                final NavigatorTree navigator = Application.getNavigator();
                final NavigatorProjectNode root = navigator.getModel().getProjectNode(new VoidProgressMonitor(), project.getContainer());

                new PersistChangesDialog(root).showDialog(JOptionPane.getRootFrame());
            }
        }

        @Override
        public boolean isEnabled(@NotNull MenuItemContext ctx) {
            final Project project = ctx.getData(CommonDataKeys.PROJECT_KEY);
            return project != null && project.getPackfileManager().hasChanges();
        }
    }

    @MenuItemRegistration(parent = APP_MENU_FILE_ID, name = "E&xit", description = "Exit the application :(", group = APP_MENU_FILE_GROUP_EXIT, order = 1000)
    public static class ExitItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            JOptionPane.getRootFrame().dispose();
        }
    }
}

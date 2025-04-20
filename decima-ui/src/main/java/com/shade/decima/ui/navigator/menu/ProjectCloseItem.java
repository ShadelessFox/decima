package com.shade.decima.ui.navigator.menu;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectManager;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.CommonDataKeys;
import com.shade.decima.ui.dialogs.PersistChangesDialog;
import com.shade.decima.ui.editor.NodeEditorInput;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.editors.SaveableEditor;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_ID, name = "Close Project", group = CTX_MENU_NAVIGATOR_GROUP_PROJECT, order = 1000)
public class ProjectCloseItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final Project project = ctx.getData(CommonDataKeys.PROJECT_KEY);

        if (confirmProjectClose(project, ctx.getData(PlatformDataKeys.EDITOR_MANAGER_KEY))) {
            ProjectManager.getInstance().closeProject(project);
        }
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof NavigatorProjectNode node && node.isOpen();
    }

    public static boolean confirmProjectClose(@NotNull Project project, @Nullable EditorManager manager) {
        if (isProjectDirty(project, manager)) {
            final int result = JOptionPane.showConfirmDialog(
                JOptionPane.getRootFrame(),
                "Do you want to save changes to project '%s'?".formatted(project.getContainer().getName()),
                "Confirm Close",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION) {
                return saveProjectChanges(new VoidProgressMonitor(), project, manager);
            } else {
                return result == JOptionPane.NO_OPTION;
            }
        }

        return true;
    }

    public static boolean isProjectDirty(@NotNull Project project, @Nullable EditorManager manager) {
        if (project.getPackfileManager().hasChanges()) {
            return true;
        }

        if (manager != null) {
            for (Editor editor : manager.getEditors()) {
                if (editor instanceof SaveableEditor e && e.isDirty() && e.getInput() instanceof NodeEditorInput i && i.getProject() == project) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean saveProjectChanges(@NotNull ProgressMonitor monitor, @NotNull Project project, @Nullable EditorManager manager) {
        if (manager != null) {
            for (Editor editor : manager.getEditors()) {
                if (editor instanceof SaveableEditor e && e.isDirty() && e.getInput() instanceof NodeEditorInput i && i.getProject() == project) {
                    e.doSave(monitor);
                }
            }
        }

        final NavigatorProjectNode node = Application.getNavigator().getModel().getProjectNode(monitor, project.getContainer());
        final PersistChangesDialog dialog = new PersistChangesDialog(node);

        return dialog.showDialog(JOptionPane.getRootFrame()) != BaseDialog.BUTTON_CANCEL;
    }
}

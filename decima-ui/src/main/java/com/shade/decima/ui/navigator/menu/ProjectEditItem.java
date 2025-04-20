package com.shade.decima.ui.navigator.menu;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.ProjectManager;
import com.shade.decima.ui.CommonDataKeys;
import com.shade.decima.ui.dialogs.ProjectEditDialog;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import javax.swing.*;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_ID, name = "Edit Project\u2026", keystroke = "ctrl alt shift S", group = CTX_MENU_NAVIGATOR_GROUP_PROJECT, order = 2000)
public class ProjectEditItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final ProjectContainer container = ctx.getData(CommonDataKeys.PROJECT_CONTAINER_KEY);
        final Project project = ctx.getData(CommonDataKeys.PROJECT_KEY);

        if (container == null) {
            return;
        }

        final ProjectEditDialog dialog = new ProjectEditDialog(true, project == null);

        dialog.load(container);

        if (dialog.showDialog(JOptionPane.getRootFrame()) == BaseDialog.BUTTON_OK) {
            dialog.save(container);
            ProjectManager.getInstance().updateProject(container);
        }
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof NavigatorProjectNode;
    }
}

package com.shade.decima.ui.navigator.menu;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.Workspace;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.CommonDataKeys;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import javax.swing.*;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_ID, name = "Delete Project", group = CTX_MENU_NAVIGATOR_GROUP_PROJECT, order = 3000)
public class ProjectDeleteItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final Workspace workspace = ctx.getData(CommonDataKeys.WORKSPACE_KEY);
        final ProjectContainer container = ctx.getData(CommonDataKeys.PROJECT_CONTAINER_KEY);

        if (workspace == null || container == null) {
            return;
        }

        final int result = JOptionPane.showConfirmDialog(
            Application.getFrame(),
            "Do you really want to delete project '%s'?".formatted(container.getName()),
            "Delete project",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            workspace.removeProject(container, true);
        }
    }

    @Override
    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        return ctx.getData(CommonDataKeys.PROJECT_KEY) == null;
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof NavigatorProjectNode;
    }
}

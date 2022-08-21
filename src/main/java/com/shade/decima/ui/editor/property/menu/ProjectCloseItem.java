package com.shade.decima.ui.editor.property.menu;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.Workspace;
import com.shade.decima.ui.CommonDataKeys;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_ID, name = "Close Project", group = CTX_MENU_NAVIGATOR_GROUP_PROJECT, order = 1000)
public class ProjectCloseItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final Workspace workspace = ctx.getData(CommonDataKeys.WORKSPACE_KEY);
        final Project project = ctx.getData(CommonDataKeys.PROJECT_KEY);

        if (workspace == null || project == null) {
            return;
        }

        workspace.closeProject(project.getContainer(), true);
    }

    @Override
    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        return ctx.getData(CommonDataKeys.PROJECT_KEY) != null;
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof NavigatorProjectNode;
    }
}

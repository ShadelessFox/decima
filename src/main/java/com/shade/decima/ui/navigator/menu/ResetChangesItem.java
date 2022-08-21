package com.shade.decima.ui.navigator.menu;

import com.shade.decima.model.app.Project;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.CommonDataKeys;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_ID, name = "Reset Changes", group = CTX_MENU_NAVIGATOR_GROUP_EDIT, order = 1000)
public class ResetChangesItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final NavigatorFileNode node = (NavigatorFileNode) ctx.getData(PlatformDataKeys.SELECTION_KEY);
        final Project project = ctx.getData(CommonDataKeys.PROJECT_KEY);

        project.getPersister().removeChange(node);
        Application.getFrame().getNavigator().getModel().fireNodeChanged(node);
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof NavigatorFileNode node
            && ctx.getData(CommonDataKeys.PROJECT_KEY).getPersister().hasChangesInPath(node);
    }
}

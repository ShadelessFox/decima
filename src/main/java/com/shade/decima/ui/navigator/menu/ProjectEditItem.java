package com.shade.decima.ui.navigator.menu;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.CommonDataKeys;
import com.shade.decima.ui.dialogs.ProjectEditDialog;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_ID, name = "Edit Project\u2026", keystroke = "ctrl alt shift S", group = CTX_MENU_NAVIGATOR_GROUP_PROJECT, order = 2000)
public class ProjectEditItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final ProjectContainer container = ctx.getData(CommonDataKeys.PROJECT_CONTAINER_KEY);

        if (container == null) {
            return;
        }

        final ProjectEditDialog dialog = new ProjectEditDialog(true);

        dialog.load(container);

        if (dialog.showDialog(Application.getInstance().getFrame()) == BaseDialog.BUTTON_OK) {
            dialog.save(container);
            Application.getProjectManager().updateProject(container);
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

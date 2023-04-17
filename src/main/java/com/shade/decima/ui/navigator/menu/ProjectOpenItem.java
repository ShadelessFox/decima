package com.shade.decima.ui.navigator.menu;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.navigator.NavigatorTreeModel;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_ID, name = "Open Project", group = CTX_MENU_NAVIGATOR_GROUP_PROJECT, order = 1000)
public class ProjectOpenItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final NavigatorTreeModel model = Application.getNavigator().getModel();
        final NavigatorProjectNode node = (NavigatorProjectNode) ctx.getData(PlatformDataKeys.SELECTION_KEY);

        // Force the node to load its children and therefore initialize itself
        model.getChild(node, 0);
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof NavigatorProjectNode node && !node.isOpen();
    }
}

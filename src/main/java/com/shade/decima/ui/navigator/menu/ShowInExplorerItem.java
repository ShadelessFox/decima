package com.shade.decima.ui.navigator.menu;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.CommonDataKeys;
import com.shade.decima.ui.UIUtils;
import com.shade.decima.ui.menu.MenuItem;
import com.shade.decima.ui.menu.MenuItemContext;
import com.shade.decima.ui.menu.MenuItemRegistration;
import com.shade.decima.ui.navigator.impl.NavigatorPackfileNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_ID, name = "Show in Explorer", group = CTX_MENU_NAVIGATOR_GROUP_GENERAL, order = 1000)
public class ShowInExplorerItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final Object selection = ctx.getData(CommonDataKeys.SELECTION_KEY);

        if (selection instanceof NavigatorPackfileNode node) {
            UIUtils.browseFileDirectory(node.getPackfile().getPath());
        } else if (selection instanceof NavigatorProjectNode node) {
            UIUtils.browseFileDirectory(node.getContainer().getExecutablePath());
        }
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        final Object selection = ctx.getData(CommonDataKeys.SELECTION_KEY);

        return selection instanceof NavigatorProjectNode
            || selection instanceof NavigatorPackfileNode;
    }
}

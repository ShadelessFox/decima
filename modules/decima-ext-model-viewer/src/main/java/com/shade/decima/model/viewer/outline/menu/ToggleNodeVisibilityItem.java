package com.shade.decima.model.viewer.outline.menu;

import com.shade.decima.model.viewer.isr.Node;
import com.shade.decima.model.viewer.outline.OutlineTreeNode;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

@MenuItemRegistration(parent = OutlineMenuConstants.CTX_MENU_SCENE_OUTLINE_ID, name = "Show node", group = OutlineMenuConstants.CTX_MENU_SCENE_OUTLINE_GROUP_GENERAL, order = 1000)
public class ToggleNodeVisibilityItem extends MenuItem implements MenuItem.Check {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final OutlineTreeNode selection = (OutlineTreeNode) ctx.getData(PlatformDataKeys.SELECTION_KEY);
        final Node node = selection.getNode();
        node.setVisible(!node.isVisible());
    }

    @Override
    public boolean isChecked(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof OutlineTreeNode node
            && node.getNode().isVisible();
    }
}

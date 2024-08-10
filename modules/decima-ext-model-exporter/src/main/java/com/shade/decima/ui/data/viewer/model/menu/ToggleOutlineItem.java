package com.shade.decima.ui.data.viewer.model.menu;

import com.shade.decima.model.viewer.ModelViewport;
import com.shade.decima.model.viewer.NodeModel;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = BAR_MODEL_VIEWER_ID, name = "Show Outline", description = "Show outline popup", icon = "Action.outlineIcon", group = BAR_MODEL_VIEWER_GROUP_MISC, order = 2000)
public class ToggleOutlineItem extends MenuItem implements MenuItem.Check {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final ModelViewport viewport = ctx.getData(ModelViewport.VIEWPORT_KEY);
        viewport.setShowOutline(!viewport.isShowOutline());
    }

    @Override
    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        return ctx.getData(ModelViewport.VIEWPORT_KEY).getModel() instanceof NodeModel;
    }

    @Override
    public boolean isChecked(@NotNull MenuItemContext ctx) {
        return ctx.getData(ModelViewport.VIEWPORT_KEY).isShowOutline();
    }
}

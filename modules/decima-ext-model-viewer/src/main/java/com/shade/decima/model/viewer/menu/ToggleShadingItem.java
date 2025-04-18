package com.shade.decima.model.viewer.menu;

import com.shade.decima.model.viewer.ModelViewport;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = BAR_MODEL_VIEWER_ID, name = "Toggle Shading", description = "Toggle flat/smooth shading", icon = "Action.shadingIcon", group = BAR_MODEL_VIEWER_GROUP_RENDER, order = 1000)
public class ToggleShadingItem extends MenuItem implements MenuItem.Check {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final ModelViewport viewport = ctx.getData(ModelViewport.VIEWPORT_KEY);
        viewport.setSoftShading(!viewport.isSoftShading());
    }

    @Override
    public boolean isChecked(@NotNull MenuItemContext ctx) {
        return ctx.getData(ModelViewport.VIEWPORT_KEY).isSoftShading();
    }
}

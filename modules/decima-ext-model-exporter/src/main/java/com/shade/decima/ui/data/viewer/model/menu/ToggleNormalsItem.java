package com.shade.decima.ui.data.viewer.model.menu;

import com.shade.decima.model.viewer.ModelViewport;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = BAR_MODEL_VIEWER_ID, name = "Toggle Normals", description = "Toggle normal vectors rendering", icon = "Action.normalsIcon", group = BAR_MODEL_VIEWER_GROUP_RENDER, order = 3000)
public class ToggleNormalsItem extends MenuItem implements MenuItem.Check {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final ModelViewport viewport = ctx.getData(ModelViewport.VIEWPORT_KEY);
        viewport.setShowNormals(!viewport.isShowNormals());
    }

    @Override
    public boolean isChecked(@NotNull MenuItemContext ctx) {
        return ctx.getData(ModelViewport.VIEWPORT_KEY).isShowNormals();
    }
}

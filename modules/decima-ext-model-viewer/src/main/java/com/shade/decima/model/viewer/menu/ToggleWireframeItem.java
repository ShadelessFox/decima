package com.shade.decima.model.viewer.menu;

import com.shade.decima.model.viewer.ModelViewport;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = BAR_MODEL_VIEWER_ID, name = "Toggle Wireframe", description = "Toggle wireframe rendering", icon = "Action.wireframeIcon", group = BAR_MODEL_VIEWER_GROUP_RENDER, order = 2000)
public class ToggleWireframeItem extends MenuItem implements MenuItem.Check {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final ModelViewport viewport = ctx.getData(ModelViewport.VIEWPORT_KEY);
        viewport.setShowWireframe(!viewport.isShowWireframe());
    }

    @Override
    public boolean isChecked(@NotNull MenuItemContext ctx) {
        return ctx.getData(ModelViewport.VIEWPORT_KEY).isShowWireframe();
    }
}

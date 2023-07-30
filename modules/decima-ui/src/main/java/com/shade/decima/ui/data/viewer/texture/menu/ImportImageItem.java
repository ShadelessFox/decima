package com.shade.decima.ui.data.viewer.texture.menu;

import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = BAR_TEXTURE_VIEWER_BOTTOM_ID, description = "Import image", icon = "Action.importIcon", group = BAR_TEXTURE_VIEWER_BOTTOM_GROUP_GENERAL, order = 2000)
public class ImportImageItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        // TODO
    }

    @Override
    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        return false;
    }
}

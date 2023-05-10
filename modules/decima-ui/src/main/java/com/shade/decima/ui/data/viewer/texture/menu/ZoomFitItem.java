package com.shade.decima.ui.data.viewer.texture.menu;

import com.shade.decima.ui.data.viewer.texture.TextureViewerPanel;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = BAR_TEXTURE_VIEWER_ID, name = "Fit image to viewport", icon = "Action.zoomFitIcon", group = BAR_TEXTURE_VIEWER_GROUP_ZOOM, order = 3000)
public class ZoomFitItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        ctx.getData(TextureViewerPanel.PANEL_KEY).fit();
    }

    @Override
    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        return ctx.getData(TextureViewerPanel.PROVIDER_KEY) != null;
    }
}

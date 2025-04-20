package com.shade.decima.ui.data.viewer.texture.menu;

import com.shade.decima.ui.data.viewer.texture.TextureViewerPanel;
import com.shade.decima.ui.data.viewer.texture.controls.ImagePanel;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = BAR_TEXTURE_VIEWER_ID, name = "Zoom Out", description = "Zoom out texture", icon = "Action.zoomOutIcon", group = BAR_TEXTURE_VIEWER_GROUP_ZOOM, order = 2000)
public class ZoomOutItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final ImagePanel panel = ctx.getData(TextureViewerPanel.PANEL_KEY);
        float zoom = TextureViewerPanel.ZOOM_MAX_LEVEL;

        while (zoom >= panel.getZoom()) {
            zoom /= 2;
        }

        panel.setZoom(zoom);
    }

    @Override
    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        return ctx.getData(TextureViewerPanel.PROVIDER_KEY) != null
            && ctx.getData(TextureViewerPanel.ZOOM_KEY) > TextureViewerPanel.ZOOM_MIN_LEVEL;
    }
}

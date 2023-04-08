package com.shade.decima.ui.data.viewer.texture.menu;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.data.viewer.texture.TextureExportDialog;
import com.shade.decima.ui.data.viewer.texture.TextureViewerPanel;
import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = BAR_TEXTURE_VIEWER_BOTTOM_ID, name = "Export image", icon = "Action.exportIcon", group = BAR_TEXTURE_VIEWER_BOTTOM_GROUP_GENERAL, order = 1000)
public class ExportImageItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final ImageProvider provider = ctx.getData(TextureViewerPanel.PANEL_KEY).getProvider();

        if (provider != null) {
            new TextureExportDialog(provider).showDialog(Application.getFrame());
        }
    }

    @Override
    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        return ctx.getData(TextureViewerPanel.PROVIDER_KEY) != null;
    }
}

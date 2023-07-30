package com.shade.decima.ui.data.viewer.texture.menu;

import com.shade.decima.ui.data.viewer.texture.TextureViewerPanel;
import com.shade.decima.ui.data.viewer.texture.controls.ImagePanelViewport;
import com.shade.platform.ui.icons.ColorIcon;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = BAR_TEXTURE_VIEWER_ID, description = "Change viewport background", group = BAR_TEXTURE_VIEWER_GROUP_VIEW, order = 1000)
public class ChangeBackgroundItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final ImagePanelViewport viewport = ctx.getData(TextureViewerPanel.VIEWPORT_KEY);
        final Color color = JColorChooser.showDialog(JOptionPane.getRootFrame(), "Choose background color", viewport.getBackground());

        if (color != null) {
            viewport.setBackground(color);
        }
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull MenuItemContext ctx) {
        final ImagePanelViewport viewport = ctx.getData(TextureViewerPanel.VIEWPORT_KEY);
        return new ColorIcon(viewport.getBackground());
    }
}

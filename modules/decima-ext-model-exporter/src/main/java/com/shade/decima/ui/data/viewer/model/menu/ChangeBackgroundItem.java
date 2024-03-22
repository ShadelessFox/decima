package com.shade.decima.ui.data.viewer.model.menu;

import com.shade.decima.model.viewer.ModelViewport;
import com.shade.platform.ui.icons.ColorIcon;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = BAR_MODEL_VIEWER_ID, name = "Change Background\u2026", description = "Change viewport background", group = BAR_MODEL_VIEWER_GROUP_GENERAL, order = 1000)
public class ChangeBackgroundItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final ModelViewport viewport = ctx.getData(ModelViewport.VIEWPORT_KEY);
        final Color color = JColorChooser.showDialog(JOptionPane.getRootFrame(), "Choose background color", viewport.getBackground());

        if (color != null) {
            viewport.setBackground(color);
        }
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull MenuItemContext ctx) {
        final ModelViewport viewport = ctx.getData(ModelViewport.VIEWPORT_KEY);
        return new ColorIcon(viewport.getBackground());
    }
}

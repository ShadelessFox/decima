package com.shade.decima.ui.data.viewer.font.menu;

import com.shade.decima.model.rtti.types.java.HwFont;
import com.shade.decima.ui.data.viewer.font.FontExportDialog;
import com.shade.decima.ui.data.viewer.font.FontViewerPanel;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import javax.swing.*;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = BAR_FONT_VIEWER_BOTTOM_ID, description = "Export font", icon = "Action.exportIcon", group = BAR_FONT_VIEWER_BOTTOM_GROUP_GENERAL, order = 1000)
public class ExportFontItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final HwFont font = ctx.getData(FontViewerPanel.FONT_KEY);
        final FontExportDialog dialog = new FontExportDialog(font);

        dialog.showDialog(JOptionPane.getRootFrame());
    }

    @Override
    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        return ctx.getData(FontViewerPanel.FONT_KEY) != null;
    }
}

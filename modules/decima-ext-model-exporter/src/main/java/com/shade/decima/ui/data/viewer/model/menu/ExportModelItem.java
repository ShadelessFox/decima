package com.shade.decima.ui.data.viewer.model.menu;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.viewer.model.ModelExportDialog;
import com.shade.decima.ui.data.viewer.model.ModelViewerPanel;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import javax.swing.*;
import java.util.Objects;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = BAR_MODEL_VIEWER_BOTTOM_ID, description = "Export Model\u2026", icon = "Action.exportIcon", group = BAR_MODEL_VIEWER_BOTTOM_GROUP_GENERAL, order = 1000)
public class ExportModelItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final ModelViewerPanel provider = ctx.getData(ModelViewerPanel.PANEL_KEY);
        final ValueController<RTTIObject> controller = Objects.requireNonNull(provider.getController());

        new ModelExportDialog(controller).showDialog(JOptionPane.getRootFrame());
    }

    @Override
    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        final ModelViewerPanel panel = ctx.getData(ModelViewerPanel.PANEL_KEY);
        return panel != null && panel.getController() != null;
    }
}

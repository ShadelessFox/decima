package com.shade.decima.model.exporter.menu;

import com.shade.decima.model.viewer.MeshViewerCanvas;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = BAR_MODEL_VIEWER_ID, name = "Normals", description = "Toggle normals rendering", group = BAR_MODEL_VIEWER_GROUP_GENERAL, order = 3000)
public class ShowNormalsItem extends MenuItem implements MenuItem.Check {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final MeshViewerCanvas canvas = ctx.getData(MeshViewerCanvas.CANVAS_KEY);
        canvas.setShowNormals(!canvas.isShowNormals());
    }

    @Override
    public boolean isChecked(@NotNull MenuItemContext ctx) {
        return ctx.getData(MeshViewerCanvas.CANVAS_KEY).isShowNormals();
    }
}

package com.shade.decima.model.exporter.menu;

import com.formdev.flatlaf.icons.FlatAbstractIcon;
import com.shade.decima.model.viewer.MeshViewerCanvas;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = BAR_MODEL_VIEWER_ID, name = "Toggle wireframe", group = BAR_MODEL_VIEWER_GROUP_GENERAL, order = 2000)
public class ToggleWireframeItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final MeshViewerCanvas canvas = ctx.getData(MeshViewerCanvas.CANVAS_KEY);
        canvas.setShowWireframe(!canvas.isShowWireframe());
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull MenuItemContext ctx) {
        final MeshViewerCanvas canvas = ctx.getData(MeshViewerCanvas.CANVAS_KEY);
        return new WireframeIcon(!canvas.isShowWireframe());
    }

    private static class WireframeIcon extends FlatAbstractIcon {
        private final boolean fill;

        public WireframeIcon(boolean fill) {
            super(16, 16, null);
            this.fill = fill;
        }

        @Override
        protected void paintIcon(Component c, Graphics2D g) {
            if (fill) {
                g.setColor(UIManager.getColor("Actions.GreyInline"));
                g.fillRect(0, 0, 16, 16);
            }

            g.setColor(UIManager.getColor("Actions.Grey"));
            g.drawRect(0, 0, 15, 15);
            g.drawLine(0, 0, 15, 15);
        }
    }
}

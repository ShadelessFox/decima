package com.shade.decima.ui.icon.overlay;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

public class FlatObjectModifiedOverlayIcon extends FlatOverlayIcon {
    public FlatObjectModifiedOverlayIcon(@NotNull Icon delegate, int x, int y) {
        super(delegate, x, y);
    }

    @Override
    protected void paintOverlay(Component c, Graphics2D g) {
        g.setColor(UIManager.getColor("Actions.Yellow"));
        g.fill(FlatUIUtils.createPath(6.8, 2.8, 6.4, 2.1, 4.4, 3.2, 4.4, 0.9, 3.6, 0.9, 3.6, 3.2, 1.6, 2.1, 1.2, 2.8, 3.1, 4, 1.2, 5.2, 1.6, 5.9, 3.6, 4.8, 3.6, 7.1, 4.4, 7.1, 4.4, 4.8, 6.4, 5.9, 6.8, 5.2, 4.9, 4));
    }

    @Nullable
    @Override
    protected Shape getOverlayMask() {
        return new Ellipse2D.Double(0.0, 0.0, 8.0, 8.0);
    }
}

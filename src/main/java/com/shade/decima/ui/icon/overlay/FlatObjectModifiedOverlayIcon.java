package com.shade.decima.ui.icon.overlay;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.shade.decima.model.util.NotNull;

import javax.swing.*;
import java.awt.*;

public class FlatObjectModifiedOverlayIcon extends FlatOverlayIcon {
    public FlatObjectModifiedOverlayIcon(@NotNull Icon delegate, @NotNull Color background, int x, int y) {
        super(delegate, background, x, y);
    }

    @Override
    protected void paintOverlay(Component c, Graphics2D g) {
        g.setColor(UIManager.getColor("Actions.Yellow"));
        g.fill(FlatUIUtils.createPath(7, 3.4, 5.4, 3.4, 6.5, 2.3, 5.7, 1.5, 4.6, 2.6, 4.6, 1, 3.4, 1, 3.4, 2.6, 2.3, 1.5, 1.5, 2.3, 2.6, 3.4, 1, 3.4, 1, 4.6, 2.6, 4.6, 1.5, 5.7, 2.3, 6.5, 3.4, 5.4, 3.4, 7, 4.6, 7, 4.6, 5.4, 5.7, 6.5, 6.5, 5.7, 5.4, 4.6, 7, 4.6));
    }
}

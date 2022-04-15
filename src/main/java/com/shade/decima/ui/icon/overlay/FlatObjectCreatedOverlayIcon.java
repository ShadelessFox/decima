package com.shade.decima.ui.icon.overlay;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.shade.decima.model.util.NotNull;

import javax.swing.*;
import java.awt.*;

public class FlatObjectCreatedOverlayIcon extends FlatOverlayIcon {
    public FlatObjectCreatedOverlayIcon(@NotNull Icon delegate, @NotNull Color background, int x, int y) {
        super(delegate, background, x, y);
    }

    @Override
    protected void paintOverlay(Component c, Graphics2D g) {
        g.setColor(UIManager.getColor("Actions.Green"));
        g.fill(FlatUIUtils.createPath(5, 1, 5, 3, 7, 3, 7, 5, 5, 5, 5, 7, 3, 7, 3, 5, 1, 5, 1, 3, 3, 3, 3, 1));
    }
}

package com.shade.decima.ui.icon.overlay;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

import javax.swing.*;
import java.awt.*;

public class FlatObjectCreatedOverlayIcon extends FlatOverlayIcon {
    public FlatObjectCreatedOverlayIcon(@NotNull Icon delegate, int x, int y) {
        super(delegate, x, y);
    }

    @Override
    protected void paintOverlay(Component c, Graphics2D g) {
        g.setColor(UIManager.getColor("Actions.Green"));
        g.fill(FlatUIUtils.createPath(7, 3, 5, 3, 5, 1, 3, 1, 3, 3, 1, 3, 1, 5, 3, 5, 3, 7, 5, 7, 5, 5, 7, 5));
    }

    @Nullable
    @Override
    protected Shape getOverlayMask() {
        return FlatUIUtils.createPath(8, 2, 6, 2, 6, 0, 2, 0, 2, 2, 0, 2, 0, 6, 2, 6, 2, 8, 6, 8, 6, 6, 8, 6);
    }
}

package com.shade.decima.ui.icon.overlay;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

import javax.swing.*;
import javax.swing.plaf.UIResource;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;

public abstract class FlatOverlayIcon implements Icon, UIResource {
    private final Icon delegate;
    private final int x;
    private final int y;

    public FlatOverlayIcon(@NotNull Icon delegate, int x, int y) {
        this.delegate = delegate;
        this.x = x;
        this.y = y;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        final Graphics2D g2 = (Graphics2D) g.create();

        try {
            FlatUIUtils.setRenderingHints(g2);

            g2.translate(x, y);

            UIScale.scaleGraphics(g2);

            final Shape mask = getOverlayMask();

            if (mask != null) {
                final Shape oldClip = g2.getClip();

                final AffineTransform at = new AffineTransform();
                at.translate(this.x, this.y);

                final Area newClip = new Area(oldClip);
                newClip.subtract(new Area(at.createTransformedShape(mask)));

                g2.setClip(newClip);

                delegate.paintIcon(c, g2, x, y);

                g2.setClip(oldClip);
            } else {
                delegate.paintIcon(c, g2, x, y);
            }

            g2.translate(this.x, this.y);

            paintOverlay(c, g2);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return delegate.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return delegate.getIconHeight();
    }

    protected abstract void paintOverlay(Component c, Graphics2D g);

    @Nullable
    protected abstract Shape getOverlayMask();
}

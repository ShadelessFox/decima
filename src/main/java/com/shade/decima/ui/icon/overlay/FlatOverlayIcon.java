package com.shade.decima.ui.icon.overlay;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;
import com.shade.decima.model.util.NotNull;

import javax.swing.*;
import javax.swing.plaf.UIResource;
import java.awt.*;

public abstract class FlatOverlayIcon implements Icon, UIResource {
    private final Icon delegate;
    private final Color background;
    private final int x;
    private final int y;

    public FlatOverlayIcon(@NotNull Icon delegate, @NotNull Color background, int x, int y) {
        this.delegate = delegate;
        this.background = background;
        this.x = x;
        this.y = y;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        delegate.paintIcon(c, g, x, y);

        final Graphics2D g2 = (Graphics2D) g.create();

        try {
            FlatUIUtils.setRenderingHints(g2);

            g2.translate(this.x, this.y);

            UIScale.scaleGraphics(g2);

            g2.setColor(background);
            g2.fillOval(0, 0, 8, 8);

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
}

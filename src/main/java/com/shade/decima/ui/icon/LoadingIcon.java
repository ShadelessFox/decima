package com.shade.decima.ui.icon;

import com.formdev.flatlaf.icons.FlatAbstractIcon;
import com.shade.decima.model.util.NotNull;

import java.awt.*;

public class LoadingIcon extends FlatAbstractIcon {
    private static final int STEPS = 8;

    private int iteration = 0;

    public LoadingIcon(int width, int height, @NotNull Color color) {
        super(width, height, color);
    }

    public void advance() {
        iteration = (iteration + 1) % STEPS;
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g2) {
        for (int i = 0; i < STEPS; i++) {
            final int ow = getIconWidth() / 4;
            final int oh = getIconHeight() / 4;
            final int ox = ow + getIconWidth() / 8;
            final int oy = oh + getIconHeight() / 8;
            final double angle = Math.PI * 2.0 * ((iteration + i) % STEPS) / STEPS;

            if (i > 0) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) i / STEPS));
            }

            g2.fillOval((int) (Math.cos(angle) * ox) + ox, (int) (Math.sin(angle) * oy) + oy, ow, oh);
        }
    }
}

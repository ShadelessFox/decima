package com.shade.platform.ui.icons;

import com.formdev.flatlaf.icons.FlatAbstractIcon;

import javax.swing.*;
import java.awt.*;

public class LoadingIcon extends FlatAbstractIcon {
    public static final int SEGMENTS = 8;

    private int iteration = 0;

    public LoadingIcon() {
        super(16, 16, UIManager.getColor("Objects.Grey"));
    }

    public void advance() {
        iteration = (iteration + 1) % SEGMENTS;
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g2) {
        for (int i = 0; i < SEGMENTS; i++) {
            final int ow = getIconWidth() / 4;
            final int oh = getIconHeight() / 4;
            final int ox = ow + getIconWidth() / 8;
            final int oy = oh + getIconHeight() / 8;
            final double angle = Math.PI * 2.0 * ((iteration + i) % SEGMENTS) / SEGMENTS;

            if (i > 0) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) i / SEGMENTS));
            }

            g2.fillOval((int) (Math.cos(angle) * ox) + ox, (int) (Math.sin(angle) * oy) + oy, ow, oh);
        }
    }
}

package com.shade.platform.ui.icons;

import com.formdev.flatlaf.icons.FlatAbstractIcon;

import javax.swing.*;
import java.awt.*;

public class LoadingIcon extends FlatAbstractIcon {
    public static final int SEGMENTS = 8;

    private static final int WIDTH = 16;
    private static final int HEIGHT = 16;

    private int iteration = 0;

    public LoadingIcon() {
        super(WIDTH, HEIGHT, UIManager.getColor("Objects.Grey"));
    }

    public void advance() {
        iteration = (iteration + 1) % SEGMENTS;
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g2) {
        for (int i = 0; i < SEGMENTS; i++) {
            final int ow = WIDTH / 4;
            final int oh = HEIGHT / 4;
            final int ox = ow + WIDTH / 8;
            final int oy = oh + HEIGHT / 8;
            final double angle = Math.PI * 2.0 * ((iteration + i) % SEGMENTS) / SEGMENTS;

            if (i > 0) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) i / SEGMENTS));
            }

            g2.fillOval((int) (Math.cos(angle) * ox) + ox, (int) (Math.sin(angle) * oy) + oy, ow, oh);
        }
    }
}

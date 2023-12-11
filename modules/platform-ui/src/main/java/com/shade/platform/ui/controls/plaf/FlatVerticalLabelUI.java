package com.shade.platform.ui.controls.plaf;

import com.formdev.flatlaf.ui.FlatLabelUI;
import com.shade.platform.ui.controls.VerticalLabel;
import com.shade.util.NotNull;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class FlatVerticalLabelUI extends FlatLabelUI {
    private final Rectangle paintIconR = new Rectangle();
    private final Rectangle paintTextR = new Rectangle();
    private final Rectangle paintViewR = new Rectangle();

    public FlatVerticalLabelUI() {
        super(false);
    }

    @NotNull
    public static ComponentUI createUI(@NotNull JComponent c) {
        return new FlatVerticalLabelUI();
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        final VerticalLabel label = (VerticalLabel) c;
        final String text = label.getText();
        final Icon icon = label.isEnabled() ? label.getIcon() : label.getDisabledIcon();

        if (icon == null && text == null) {
            return;
        }

        final Insets insets = c.getInsets();
        final FontMetrics fm = g.getFontMetrics();

        paintViewR.x = insets.left;
        paintViewR.y = insets.top;
        paintViewR.height = c.getWidth() - (insets.left + insets.right);
        paintViewR.width = c.getHeight() - (insets.top + insets.bottom);

        paintIconR.x = paintIconR.y = paintIconR.width = paintIconR.height = 0;
        paintTextR.x = paintTextR.y = paintTextR.width = paintTextR.height = 0;

        final String clippedText = layoutCL(label, fm, text, icon, paintViewR, paintIconR, paintTextR);
        final Graphics2D g2 = (Graphics2D) g;
        final AffineTransform tr = g2.getTransform();

        if (icon != null) {
            icon.paintIcon(c, g, paintIconR.x, paintIconR.y + c.getHeight() - icon.getIconHeight());
        }

        if (label.isClockwise()) {
            g2.rotate(Math.PI / 2);
            g2.translate(0, -c.getWidth());
        } else {
            g2.rotate(-Math.PI / 2);
            g2.translate(-c.getHeight(), 0);
        }

        if (text != null) {
            final int textX = paintTextR.x;
            final int textY = paintTextR.y + fm.getAscent();

            if (label.isEnabled()) {
                paintEnabledText(label, g, clippedText, textX, textY);
            } else {
                paintDisabledText(label, g, clippedText, textX, textY);
            }
        }

        g2.setTransform(tr);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public Dimension getPreferredSize(JComponent c) {
        final Dimension size = super.getPreferredSize(c);
        return new Dimension(size.height, size.width);
    }
}

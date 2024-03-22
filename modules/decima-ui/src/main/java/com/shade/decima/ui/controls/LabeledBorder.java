package com.shade.decima.ui.controls;

import com.shade.platform.ui.UIColor;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.border.Border;
import java.awt.*;

public record LabeledBorder(@NotNull String label) implements Border {
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        final Graphics2D g2 = (Graphics2D) g.create();

        UIUtils.setRenderingHints(g2);

        final Font font = g2.getFont();
        final FontMetrics metrics = g2.getFontMetrics(font);

        g2.setColor(c.getForeground());
        g2.drawString(label, x, y + metrics.getAscent());

        g2.setColor(UIColor.SHADOW);
        g2.drawLine(x + metrics.stringWidth(label) + 5, y + metrics.getHeight() / 2, x + width, y + metrics.getHeight() / 2);

        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        final int height = c.getFontMetrics(c.getFont()).getHeight();
        return new Insets(height + 1, 0, 0, 0);
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}

package com.shade.decima.ui.controls;

import com.shade.util.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public record LabeledBorder(@NotNull JLabel label) implements Border {
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        final Dimension size = label.getPreferredSize();

        g.setColor(UIManager.getColor("Separator.foreground"));
        g.drawLine(x + size.width + 5, size.height / 2, x + width, size.height / 2);

        g.translate(x, y);
        label.setSize(width, size.height);
        label.paint(g);
        g.translate(-x, -y);
    }

    @Override
    public Insets getBorderInsets(Component c) {
        final Dimension size = label.getPreferredSize();
        return new Insets(1 + size.height, 0, 0, 0);
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}

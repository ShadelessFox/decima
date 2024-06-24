package com.shade.decima.ui.data.viewer.texture.controls;

import com.shade.platform.ui.UIColor;
import com.shade.platform.ui.icons.ColorIcon;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;

public class ImagePanelViewport extends JViewport {
    private static final int TILE_SIZE = 8;

    public ImagePanelViewport(@NotNull ImagePanel panel) {
        final JPanel inner = new JPanel();
        inner.setLayout(new GridBagLayout());
        inner.setOpaque(false);
        inner.add(panel);

        setView(inner);
        setBackground(UIColor.TRANSPARENT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        final Color background = getBackground();
        for (int x = 0; x < getWidth(); x += TILE_SIZE) {
            for (int y = 0; y < getHeight(); y += TILE_SIZE) {
                final int i = (x + y % (TILE_SIZE * 2)) % (TILE_SIZE * 2);
                g.setColor(ColorIcon.getColor(background, i > 0));
                g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
            }
        }
    }
}

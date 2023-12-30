package com.shade.decima.ui.controls.graph;

import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;

public class GraphViewport extends JViewport {
    private static final int TILE_SIZE = 20;

    public GraphViewport(@NotNull GraphComponent component) {
        final JPanel inner = new JPanel();
        inner.setLayout(new GridBagLayout());
        inner.setOpaque(false);
        inner.add(component);

        setView(inner);
        setBackground(new Color(0, 0, 0, 0));
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(UIManager.getColor("Graph.viewportBackground"));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(UIManager.getColor("Graph.viewportGridColor"));

        final Point shift = getViewPosition();

        for (int y = TILE_SIZE / 2 - shift.y % TILE_SIZE; y < getHeight(); y += TILE_SIZE) {
            for (int x = TILE_SIZE / 2 - shift.x % TILE_SIZE; x < getWidth(); x += TILE_SIZE) {
                g.drawLine(x - 1, y, x + 1, y);
                g.drawLine(x, y - 1, x, y + 1);
            }
        }
    }
}

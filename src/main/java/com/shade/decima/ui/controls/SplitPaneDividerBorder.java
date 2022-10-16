package com.shade.decima.ui.controls;

import com.formdev.flatlaf.ui.FlatBorder;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import java.awt.*;

public class SplitPaneDividerBorder extends FlatBorder {
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        final JSplitPane pane = ((BasicSplitPaneDivider) c).getBasicSplitPaneUI().getSplitPane();

        g.setColor(UIManager.getColor("Separator.foreground"));

        if (pane.getLeftComponent() != null) {
            g.fillRect(x, y, 1, height);
        }

        if (pane.getRightComponent() != null) {
            g.fillRect(x + width - 1, y, 1, height);
        }
    }
}
